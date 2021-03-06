package jp.juggler.subwaytooter.api.entity

import android.content.Context
import android.text.Spannable
import jp.juggler.subwaytooter.api.TootParser
import org.json.JSONObject

import java.util.ArrayList
import java.util.regex.Pattern

import jp.juggler.subwaytooter.util.*

@Suppress("MemberVisibilityCanPrivate")
class NicoEnquete(
	parser : TootParser,
	status : TootStatus,
	list_attachment : ArrayList<TootAttachmentLike>?,
	src : JSONObject
) {
	
	// one of enquete,enquete_result
	val type : String?
	
	val question : String? // HTML text
	
	val decoded_question : Spannable // 表示用にデコードしてしまうのでNonNullになる
	
	// array of text with emoji
	val items : ArrayList<Choice>?
	
	// 結果の数値 // null or array of number
	private val ratios : ArrayList<Float>?
	
	// 結果の数値のテキスト // null or array of string
	private val ratios_text : ArrayList<String>?
	
	// 以下はJSONには存在しないが内部で使う
	val time_start : Long
	val status_id : Long
	
	init {
		this.type = src.parseString("type")
		
		this.question = src.parseString("question")
		this.decoded_question = DecodeOptions(
			parser.context,
			parser.linkHelper,
			short = true,
			decodeEmoji = true,
			attachmentList = list_attachment,
			linkTag = status,
			emojiMapCustom = status.custom_emojis,
			emojiMapProfile = status.profile_emojis
		).decodeHTML(this.question ?: "?")
		
		this.items = parseChoiceList(parser.context, status, parseStringArray(src, "items"))
		
		this.ratios = parseFloatArray(src, "ratios")
		this.ratios_text = parseStringArray(src, "ratios_text")
		
		this.time_start = status.time_created_at
		this.status_id = status.id
		
	}
	
	class Choice(val text : String, val decoded_text : Spannable)
	
	companion object {
		internal val log = LogCategory("NicoEnquete")
		
		const val ENQUETE_EXPIRE = 30000L
		
		const val TYPE_ENQUETE = "enquete"
		
		@Suppress("unused")
		const val TYPE_ENQUETE_RESULT = "enquete_result"
		
		@Suppress("HasPlatformType")
		private val reWhitespace = Pattern.compile("[\\s\\t\\x0d\\x0a]+")
		
		fun parse(
			parser : TootParser,
			status : TootStatus,
			list_attachment : ArrayList<TootAttachmentLike>?,
			jsonString : String?
		) : NicoEnquete? {
			jsonString ?: return null
			return try {
				NicoEnquete(
					parser,
					status,
					list_attachment,
					jsonString.toJsonObject()
				)
			} catch(ex : Throwable) {
				log.trace(ex)
				null
			}
		}
		
		private fun parseStringArray(src : JSONObject, name : String) : ArrayList<String>? {
			val array = src.optJSONArray(name)
			if(array != null) {
				val dst = array.toStringArrayList()
				if(dst.isNotEmpty()) return dst
			}
			return null
		}
		
		private fun parseFloatArray(src : JSONObject, name : String) : ArrayList<Float>? {
			val array = src.optJSONArray(name)
			if(array != null) {
				val size = array.length()
				val dst = ArrayList<Float>(size)
				for(i in 0 until size) {
					val dv = array.optDouble(i)
					dst.add(dv.toFloat())
				}
				if(dst.isNotEmpty()) return dst
			}
			return null
		}
		
		private fun parseChoiceList(
			context : Context,
			status : TootStatus,
			stringArray : ArrayList<String>?
		) : ArrayList<Choice>? {
			if(stringArray != null) {
				val size = stringArray.size
				val items = ArrayList<Choice>(size)
				val options = DecodeOptions(
					context,
					emojiMapCustom = status.custom_emojis,
					emojiMapProfile = status.profile_emojis
				)
				for(i in 0 until size) {
					val text = reWhitespace
						.matcher(stringArray[i].sanitizeBDI())
						.replaceAll(" ")
					val decoded_text = options.decodeEmoji(text)
					
					items.add(Choice(text, decoded_text))
				}
				if(items.isNotEmpty()) return items
			}
			return null
		}
	}
}
