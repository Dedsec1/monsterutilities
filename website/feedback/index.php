<?php

$file = $_FILES['log'];

$subject = $_POST['subject'];
$message = $_POST['message'];

if(!isset($subject) || !isset($message)) {
	header("HTTP/1.0 400 Bad Request");
	exit;
}

$folder = time()." - ".$subject;
mkdir($folder);
file_put_contents($folder."/message.txt", $message);

if($file['size'] > 100000) {
	header("HTTP/1.0 413 Request Entity Too Large");
} elseif(move_uploaded_file($file['tmp_name'], $folder . "/" . basename($file['name']))) {
	header("HTTP/1.0 200 OK");
} else {
	header("HTTP/1.0 500 Internal Server Error");
}

?>
