package jp.juggler.subwaytooter.api.entity

import jp.juggler.subwaytooter.util.notEmptyOrThrow
import jp.juggler.subwaytooter.util.parseLong

import org.json.JSONObject

class TootMention(
	val id : Long, // Account ID
	val url : String, // URL of user's profile (can be remote)
	val acct : String, // Equals username for local users, includes @domain for remote ones
	val username : String // The username of the account
) {
	
	constructor(src : JSONObject) : this(
		id = src.parseLong("id") ?: - 1L,
		url = src.notEmptyOrThrow("url"),
		acct = src.notEmptyOrThrow("acct"),
		username = src.notEmptyOrThrow("username")
	
	)
	
}
