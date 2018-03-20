<?php

function println($arg = "") {
	echo($arg);
	echo "<br>";
}

$latest = file_get_contents("latest/index.html");
$ver = $_GET['version']
$version = isset($ver) && $ver != "latest" ? $ver : $latest
if(isset($_GET['download'])) {

}

echo '<a href="/downloads?version='.$latest.'&download">'.$latest.'</a>';

?>
