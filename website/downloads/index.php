<?php

function println($arg = "") {
	echo($arg);
	echo "<br>";
}

$cache = array();
function get($name) {
	if($cache[$name] == null)
		$cache[$name] = file_get_contents($name);
	return $cache[$name];
}

$ver = $_GET['version'];
$version = isset($ver) ? switch($ver) {
	case "latest":
	case "unstable":
		get($ver)
		break;
	default:
		$ver
} : get("latest")

if(isset($_GET['download'])) {
	$filename = glob("*".$version.".jar")[0];
	header('Content-Disposition: attachment; filename="'.$filename.'"');
	header('Content-Transfer-Encoding: binary');
	header('Content-Type: application/force-download');
	readfile($filename);
} else {
	echo $version;
}

?>
