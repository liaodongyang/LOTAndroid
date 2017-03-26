package com.openlib.photopicker.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;

import com.openlib.photopicker.Application;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import libcore.io.DiskLruCache;

/**
 * @Class: ImageLoader
 * @Description:
 */
public class ImageLoader {

    private static final int THREAD_POOL_SIZE = 10;
    private final static Executor BITMAP_LOAD_EXECUTOR = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final static Executor BITMAP_CACHE_EXECUTOR = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /** {@value */
    public static final Bitmap.CompressFormat DEFAULT_COMPRESS_FORMAT = Bitmap.CompressFormat.PNG;
    /** {@value */
    public static final int DEFAULT_COMPRESS_QUALITY = 100;

    private LruCache<String, Bitmap> mMemoryCache;
    DiskLruCache mDiskLruCache = null;
    //
    private LinkedList<BitmapLoadTask> mTaskQueue;
    private volatile Semaphore mPoolSemaphore;
    private Handler mHandler;

    private Thread mPoolThread;
    private Handler mPoolThreadHander;
    private volatile Semaphore mSemaphore = new Semaphore(0);
    private static ImageLoader mInstance;
    private int mWidth;

    private ImageLoader() {
        init();
    }

    private void init() {
        initMemoryCache();
        initDiskCache();
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ImageHolder holder = (ImageHolder) msg.obj;
                String path = holder.path;
                ImageView imageView = holder.imageView;
                Bitmap bitmap = holder.bitmap;
                if(imageView == null || bitmap == null) {
                    return;
                }
                if (!TextUtils.isEmpty(path) && path.equals(imageView.getTag().toString())) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        };

        mPoolThread = new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mPoolThreadHander = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        try {
                            mPoolSemaphore.acquire();
                        } catch (InterruptedException e) {
                        }
                        BitmapLoadTask task = getTask();
                        if(task != null) {
                            task.executeOnExecutor(BITMAP_LOAD_EXECUTOR, mWidth, mWidth);
                        }
                    }
                };
                // sxs
                mSemaphore.release();
                Looper.loop();
            }
        };
        mPoolThread.start();

        mTaskQueue = new LinkedList<BitmapLoadTask>();
        mPoolSemaphore = new Semaphore(THREAD_POOL_SIZE);
    }

    public static synchronized ImageLoader getInstance() {
        if(mInstance == null) {
            mInstance = new ImageLoader();
        }
        return mInstance;
    }

    /**
     * init memory
     */
    public void initMemoryCache() {

        // Set up memory cache
        if (mMemoryCache != null) {
            try {
                clearMemoryCache();
            } catch (Throwable e) {
            }
        }
        // find the max memory size of the system
        int maxMemory = (int) Runtime.getRuntime().maxMemory();
        int cacheSize = maxMemory / 8;
        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {

            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                if (bitmap == null) return 0;
                return bitmap.getRowBytes() * bitmap.getHeight();
            }
        };
    }

    /**
     * initDisk
     */
    private void initDiskCache() {
        try {
            File cacheDir = OtherUtils.getDiskCacheDir(Application.getContext(), "images");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }
            mDiskLruCache = DiskLruCache.open(cacheDir, OtherUtils.getAppVersion(Application.getContext()),
                    1, 15 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void display(String path, ImageView imageView, int width, int height) {
        if (TextUtils.isEmpty(path) || imageView == null) {
            throw new IllegalArgumentException("args may not be null");
        }
        mWidth = width;
        imageView.setTag(path);
        Bitmap bitmap = getBitmapFromMemoryCache(path);
        if (bitmap == null) {
            //load from folder
            BitmapLoadTask bitmapLoadTask = new BitmapLoadTask(path, imageView);
            addTask(bitmapLoadTask);
        } else {
            ImageHolder imageHolder = new ImageHolder();
            imageHolder.bitmap = bitmap;
            imageHolder.imageView = imageView;
            imageHolder.path = path;
            Message msg = Message.obtain();
            msg.obj = imageHolder;
            mHandler.sendMessage(msg);
        }
    }

    private synchronized void addTask(BitmapLoadTask task) {
        try {
            //
            if (mPoolThreadHander == null) {
                mSemaphore.acquire();
            }
        } catch (InterruptedException e) {
        }
        mTaskQueue.add(task);
        mPoolThreadHander.sendEmptyMessage(0);
    }

    private synchronized BitmapLoadTask getTask() {
        return mTaskQueue.removeLast();
    }

    /**
     * @param key
     * @return
     */
    private Bitmap getBitmapFromMemoryCache(String key) {
        return mMemoryCache.get(key);
    }

    private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if(!TextUtils.isEmpty(key) && bitmap != null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public void clearMemoryCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
        }
    }

    private class ImageHolder {
        Bitmap bitmap;
        ImageView imageView;
        String path;
    }

    public class BitmapLoadTask extends AsyncTask<Integer, Object, Bitmap> {

        private final String path;
        private final WeakReference<ImageView> containerReference;

        public BitmapLoadTask(String path, ImageView container) {
            if (container == null || path == null) {
                throw new IllegalArgumentException("args may not be null");
            }
            this.path = path;
            this.containerReference = new WeakReference<ImageView>(container);
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            Bitmap bitmap = null;
            bitmap = tryLoadFromDiskCache(path);

            if(bitmap == null) {
                bitmap = decodeSampledBitmapFromFile(path, params[0],
                        params[1]);
                BITMAP_CACHE_EXECUTOR.execute(new DiskCacheThread(path, bitmap));
            }
            mPoolSemaphore.release();
            addBitmapToMemoryCache(path, bitmap);
            bitmap = getBitmapFromMemoryCache(path);
            return bitmap;
        }

        @Override
        protected void onCancelled(Bitmap bitmap) {

        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageHolder imageHolder = new ImageHolder();
            imageHolder.bitmap = bitmap;
            imageHolder.imageView = containerReference.get();
            imageHolder.path = path;
            Message msg = Message.obtain();
            msg.obj = imageHolder;
            mHandler.sendMessage(msg);
        }
    }

    class DiskCacheThread extends Thread {

        String path;
        Bitmap bitmap;

        public DiskCacheThread(String path, Bitmap bitmap) {
            this.path = path;
            this.bitmap = bitmap;
        }

        @Override
        public void run() {
            try {
                String key = OtherUtils.hashKeyForDisk(path);
                DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                if (editor != null) {
                    OutputStream outputStream = editor.newOutputStream(0);
                    if (cacheBitmap2Disk(bitmap, outputStream)) {
                        editor.commit();
                    } else {
                        editor.abort();
                    }
                }
                mDiskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private Bitmap tryLoadFromDiskCache(String path) {
        Bitmap bitmap = null;
        try {
            String key = OtherUtils.hashKeyForDisk(path);
            DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
            if (snapShot != null) {
                InputStream is = snapShot.getInputStream(0);
                bitmap = BitmapFactory.decodeStream(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private boolean cacheBitmap2Disk(Bitmap bitmap, OutputStream outputStream) {
        bitmap.compress(DEFAULT_COMPRESS_FORMAT, DEFAULT_COMPRESS_QUALITY, outputStream);
        return true;
    }



    /**
     */
    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight) {
        //
        int width = options.outWidth;
        int height = options.outHeight;
        int inSampleSize = 1;

        // int min = Math.min(width, height);
        // int maxReq = Math.max(reqWidth, reqHeight);

        // if(min > maxReq) {
        //     inSampleSize = Math.round((float) min / (float) maxReq);
        // }
        
        //
        if (width > reqWidth || height > reqHeight) {
            int widthRadio = Math.round(width * 1.0f / reqWidth);
            int heightRadio = Math.round(height * 1.0f / reqHeight);

            inSampleSize = Math.max(widthRadio, heightRadio);
        }

        return inSampleSize;
    }

    /**
     * @param pathName
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private Bitmap decodeSampledBitmapFromFile(String pathName,
                                               int reqWidth, int reqHeight) {
        //
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, options);
        //
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);
        //
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(pathName, options);

        return bitmap;
    }

}
