<?php
   echo "ok";
   $local_path = "./Cache/";
   if(!is_dir($local_path)){
     mkdir($local_path,0777,true);
   }
   if(is_uploaded_file($_FILES["uploadedfile"]["tmp_name"])){
	$pic_name = basename( $_FILES["uploadedfile"]["name"]);
   	$target_path = $local_path.$pic_name;
       	if(move_uploaded_file($_FILES["uploadedfile"]["tmp_name"],$target_path)){
         	echo "The file has been uploaded";
   	}else{
	         print_r($_FILES);
   	}
   }
?>
