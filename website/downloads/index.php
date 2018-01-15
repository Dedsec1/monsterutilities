<?php

function println($arg = "") {
	echo($arg);
	echo "<br>";
}

$dirs = glob("*", GLOB_ONLYDIR);
var_dump($dirs);
$size = count($dirs);
println();
println(file_get_contents("latest/index.html"));

?>