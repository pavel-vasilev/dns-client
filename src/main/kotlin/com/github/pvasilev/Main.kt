package com.github.pvasilev

fun main() {
	try {
		val hostname = "google.com"
		val address = dns.lookup(hostname)
		println("IP address for $hostname => $address")
	} catch (e: Throwable) {
		e.printStackTrace()
	}
}