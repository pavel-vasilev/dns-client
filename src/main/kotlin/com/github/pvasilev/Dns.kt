package com.github.pvasilev

val dns: Dns = Dns.DEFAULT

class Dns {

	fun lookup(hostname: String): String {
		TODO()
	}

	companion object {
		val DEFAULT = Dns()
	}
}