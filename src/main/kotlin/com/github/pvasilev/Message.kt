package com.github.pvasilev

sealed class Message(val header: Header)

class Question(
	header: Header,
	val qname: String,
	val qtype: Qtype,
	val qclass: Qclass,
) : Message(header) {

	enum class Qtype {
		A,
		AAAA,
		CNAME
	}

	enum class Qclass {
		INET
	}
}

class Answer(header: Header) : Message(header)

class Header(
	val id: Short,
	val qr: Qr,
	val opcode: Opcode,
	val aa: Boolean,
	val tc: Boolean,
	val rd: Boolean,
	val ra: Boolean,
	val rcode: Rcode,
	val qdCount: Short,
	val anCount: Short,
	val nsCount: Short,
	val arCount: Short
) {

	enum class Qr {
		QUERY,
		RESPONSE
	}

	enum class Opcode {
		QUERY,
		IQUERY,
		STATUS
	}

	enum class Rcode {
		NO_ERROR,
		SERVER_FAILURE,
		NAME_ERROR,
		NOT_IMPLEMENTED,
		REFUSED
	}
}