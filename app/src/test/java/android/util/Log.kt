package android.util

/**
 * Mock Log.class
 */
class Log {
    companion object {
        @JvmStatic
        fun d(tag: String?, msg: String): Int {
            System.out.println("DEBUG: $tag: $msg");
            return 0;
        }

        @JvmStatic
        fun i(tag: String?, msg: String): Int {
            System.out.println("INFO: $tag: $msg");
            return 0;
        }

        @JvmStatic
        fun w(tag: String?, msg: String): Int {
            System.out.println("WARNING: $tag: $msg");
            return 0;
        }

        @JvmStatic
        fun e(tag: String?, msg: String): Int {
            System.out.println("ERROR: $tag: $msg");
            return 0;
        }

        @JvmStatic
        fun getStackTraceString(e: Throwable): String {
            return e.message ?: "";
        }
    }
}