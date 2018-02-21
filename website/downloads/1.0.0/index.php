<?php

$filename = "MonsterUtilities.jar";
header('Content-Disposition: attachment; filename="MonsterUtilities.jar"');
header('Content-Transfer-Encoding: binary');
header('Content-Type: application/force-download');
readfile($filename);

?>