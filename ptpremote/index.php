<?php
if($_SERVER['REQUEST_METHOD']=='GET') {
	header("Location: index.php");
	exit();
}

$dest_host = $_POST['dest_host'];
$dest_port = $_POST['dest_port'];
if($dest_port == 443) {
	if(in_array('ssl', stream_get_transports())) {
		$dest_host="ssl://".$dest_host;
	} else {
		$dest_port=80;
	}
}
$request = base64_decode($_POST['request_data']);

header("Content-type: application/octet-stream");
$fp = fsockopen($dest_host, $dest_port, $errno, $errstr, 30);
if (!$fp) {
	echo "$errstr ($errno)<br />\n";
} else {
	fwrite($fp, $request);
	while (!feof($fp)) {
		$buffer = fread($fp, 1024);
		echo $buffer;
	}
	fclose($fp);
}
?>
