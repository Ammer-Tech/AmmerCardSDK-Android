package tech.ammer.sdk.card

import android.app.Activity
import java.lang.Exception

class TrustodyCardController private constructor(activity: Activity) {
    private var activity: Activity? = activity
    private var type: ReaderMode? = null
    private var listener: CardControllerListener? = null
    private var listenerV2: CardControllerListenerV2? = null

    companion object {
        fun Builder(activity: Activity): TrustodyCardController {
            val controller = TrustodyCardController(activity)

            return controller
        }
    }

    fun setType(androidDefault: ReaderMode): TrustodyCardController {
        type = androidDefault
        return this
    }

    fun addListener(listener: CardControllerListener): TrustodyCardController {
        this.listener = listener
        return this
    }

    fun build(): NFCCardController? {
        if (type == null || listener == null || activity == null || listenerV2 == null) {
            listenerV2?.onErrorAttach(ExceptionCode.BAD_BUILDER)
            return null
        }

        val controller = when (type!!) {
            ReaderMode.ANDROID_DEFAULT -> NFCCardController(listener!!, listenerV2!!)
            ReaderMode.SUNME -> NFCCardController(listener!!, listenerV2!!)
        }

        controller.open(activity!!)

        return controller
    }

    fun addListenerV2(listener: CardControllerListenerV2): TrustodyCardController {
        this.listenerV2 = listener
        return this
    }

    fun setTimeoutListener(l: Long): TrustodyCardController {
        return this
    }


}