package com.acurast.attested.executor.utils

import android.content.Context
import com.acurast.attested.executor.Constants
import java.net.URL

/**
 * This class implements helper methods for handling IPFS URI's.
 */
class IPFS {
    companion object {
        /**
         * Fetch IPFS content.
         */
        fun resolveUri(
            context: Context,
            ipfsUri: String,
            successCallback: (String) -> Unit,
            errorCallback: (Exception) -> Unit
        ) {
            val sharedPreferences = context.getSharedPreferences(
                Constants.CACHE_SHARED_PREFERENCES_KEY,
                Context.MODE_PRIVATE
            )

            if (sharedPreferences.contains(ipfsUri)) {
                successCallback(sharedPreferences.getString(ipfsUri, "")!!)
            } else {
                Networking.httpsGetString(
                    URL("${Constants.IPFS_NODE}${uriToHash(ipfsUri)}"),
                    mapOf(), { script, _ ->
                        // TODO: Validate IPFS CID.
                        // With certificate pinning this should not be necessary
                        //
                        // Details: https://github.com/multiformats/cid/blob/ef1b2002394b15b1e6c26c30545fd485f2c4c138/README.md#decoding-algorithm
                        //
                        // The same content can be indexed by different CID's
                        // - https://discuss.ipfs.tech/t/how-to-calculate-file-directory-hash/777

                        successCallback(script)
                        with(sharedPreferences.edit()) {
                            putString(ipfsUri, script)
                            apply()
                        }
                    }, errorCallback
                )
            }
        }

        /**
         * Remove `ipfs://` prefix from the IPFS URI.
         */
        fun uriToHash(ipfsUri: String): String {
            return ipfsUri.replace(Constants.IPFS_SCHEMA, "")
        }
    }
}