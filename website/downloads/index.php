<?php

function println($arg = "") {
	echo($arg);
	echo "<br>";
}

$latest = file_get_contents("latest/index.html");
echo '<a href="/downloads/'.$latest.'">'.$latest.'</a>';

?>