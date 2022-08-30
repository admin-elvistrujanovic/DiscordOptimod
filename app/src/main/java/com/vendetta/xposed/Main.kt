package com.vendetta.xposed

import android.annotation.SuppressLint
import android.content.res.AssetManager
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.net.URL

class InsteadHook(private val hook: (MethodHookParam) -> Any?) : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) {
        param.result = hook(param)
    }
}

class Main : IXposedHookLoadPackage {
    @SuppressLint("PrivateApi", "BlockedPrivateApi")
    override fun handleLoadPackage(param: XC_LoadPackage.LoadPackageParam) {
        if (param.packageName != "com.discord") return
        val cache = File(param.appInfo.dataDir, "cache")

        val catalystInstanceImpl = param.classLoader.loadClass("com.facebook.react.bridge.CatalystInstanceImpl")

        val loadScriptFromAssets = catalystInstanceImpl.getDeclaredMethod(
            "loadScriptFromAssets",
            AssetManager::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType
        )

        val loadScriptFromFile = catalystInstanceImpl.getDeclaredMethod(
            "jniLoadScriptFromFile",
            String::class.java,
            String::class.java,
            Boolean::class.javaPrimitiveType
        ).apply { isAccessible = true }

        XposedBridge.hookMethod(loadScriptFromAssets, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                val vendetta = File(cache, "vendetta.js")
                vendetta.writeBytes(URL("http://localhost:3000/vendetta.js").readBytes())
                loadScriptFromFile.invoke(param.thisObject, vendetta.absolutePath, vendetta.absolutePath, param.args[2])
            }
        })
    }
}
