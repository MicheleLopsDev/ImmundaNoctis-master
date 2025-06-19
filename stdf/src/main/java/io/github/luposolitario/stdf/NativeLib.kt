package io.github.luposolitario.stdf

class NativeLib {

    /**
     * A native method that is implemented by the 'stdf' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'stdf' library on application startup.
        init {
            System.loadLibrary("stdf")
        }
    }
}
