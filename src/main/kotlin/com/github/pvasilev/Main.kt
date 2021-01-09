package com.github.pvasilev

fun main() {
	try {
		val hostname = "google.com"
		val addresses = dns.lookup(hostname)
		addresses.forEach { address ->
			println("Name: $hostname")
			println("Address: $address")
		}
	} catch (e: Throwable) {
		e.printStackTrace()
	}
}