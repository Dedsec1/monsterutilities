<?php

$filename = glob("*.jar")[0];
header('Content-Disposition: attachment; filename="'.$filename.'"');
header('Content-Transfer-Encoding: binary');
header('Content-Type: application/force-download');
readfile($filename);

?>
