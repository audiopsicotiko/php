<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head profile="http://gmpg.org/xfn/11">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<meta name="google-site-verification"
	content="8RxwY8OzQyC4XLdbe361fAXgdQVaiUtPAinH1OO67Wo" />
<title>PTP Test</title>
<style type="text/css">
body {
	font-family: Arial;
	font-size: 12px
}

textarea {
	width: 800px;
	height: 500px;
	font-family: Arial;
	font-size: 10px
}

.test_ok {
	background: green;
	text-align:center;
	width:300px;
}
.test_failed {
	background: red;
	text-align:center;
	width:300px;
}
</style>
</head>
<body>
<h1>PTP Test</h1>
<?php
function head_test($dest_host, $dest_port) {
	if($dest_port == 443)
		$fp = fsockopen("ssl://".$dest_host, $dest_port, $errno, $errstr, 5);
	else
		$fp = fsockopen($dest_host, $dest_port, $errno, $errstr, 5);
	if (!$fp) {
		echo "<div class=\"test_failed\">".$dest_host.":".$dest_port." Failed"."</div>";
	} else {
		$request = "HEAD / HTTP/1.1\r\nHOST: ".$dest_host."\r\n"."Connection: close\r\n"."\r\n";
		fwrite($fp, $request);
		while (!feof($fp)) {
			$buffer .= fread($fp, 1024);
			
		}
		fclose($fp);
		if(strpos($buffer, "HTTP/1.1") == 0)
			echo "<div class=\"test_ok\">".$dest_host.":".$dest_port." OK"."</div>";
		else
			echo "<div class=\"test_failed\">".$dest_host.":".$dest_port." Failed"."</div>";
	}
}

head_test("www.sina.com.cn", 80);
head_test("www.baidu.com", 80);
head_test("www.google.com", 80);
head_test("www.yahoo.com", 80);
head_test("mail.google.com", 443);
head_test("www.facebook.com", 80);
head_test("www.facebook.com", 443);
head_test("www.twitter.com", 80);
head_test("www.twitter.com", 443);
head_test("69.63.189.16", 80);
head_test("69.63.189.16", 443);
head_test("168.143.162.68", 80);
head_test("168.143.162.68", 443);
?>
</body>
</html>
