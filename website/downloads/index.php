<?php

function println ($string_message) {
    $_SERVER['SERVER_PROTOCOL'] ? print "$string_message<br />" : print "$string_message\n";
}

$cache = array();
function get($name) {
	if($cache[$name] == null)
		$cache[$name] = trim(file_get_contents($name));
	return $cache[$name];
}

$version = $_GET['version'];
if(isset($version)) {
	if($version == "latest" || $version == "unstable")
		$version = get($version);
} else
	$version = get("latest");

if(isset($_GET['download'])) {
	$filename = end(glob("files/*".$version.".jar"));
	$size = filesize($filename);
	header('Content-Disposition: attachment; filename="'.end(explode("/", $filename)).'"');
	header('Content-Transfer-Encoding: binary');
	header('Content-Type: application/force-download');
	header("Content-Length: $size");
	readfile($filename);
} else {
	echo $version;
}

?>
