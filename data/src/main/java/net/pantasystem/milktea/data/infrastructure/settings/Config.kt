@file:Suppress("UNCHECKED_CAST")

package net.pantasystem.milktea.data.infrastructure.settings

import android.content.SharedPreferences
import net.pantasystem.milktea.model.setting.*

fun RememberVisibility.Keys.str(): String {
    return when (this) {
        is RememberVisibility.Keys.IsLocalOnly -> "accountId:${accountId}:IS_LOCAL_ONLY"
        is RememberVisibility.Keys.IsRememberNoteVisibility -> "IS_LEARN_NOTE_VISIBILITY"
        is RememberVisibility.Keys.NoteVisibility -> "accountId:${accountId}:NOTE_VISIBILITY"
    }
}

fun SharedPreferences.getPrefTypes(keys: Set<Keys> = Keys.allKeys): Map<Keys, PrefType?> {
    val strKeys = keys.associateBy { it.str() }
    return all.filter {
        strKeys.contains(it.key)
    }.map {
        strKeys[it.key]?.let { key ->
            key to it.value?.let { value ->
                when(value::class) {
                    Boolean::class -> {
                        PrefType.BoolPref(value as Boolean)
                    }
                    String::class -> {
                        PrefType.StrPref(value as String)
                    }
                    Int::class -> {
                        PrefType.IntPref(value as Int)
                    }
                    else -> null
                }
            }
        }
    }.filterNotNull().toMap()
}

fun Config.Companion.from(map: Map<Keys, PrefType?>): Config {
    return Config(
        isSimpleEditorEnabled = map.getValue<PrefType.BoolPref>(Keys.IsSimpleEditorEnabled)?.value
            ?: DefaultConfig.config.isSimpleEditorEnabled,
        reactionPickerType = (map.getValue<PrefType.IntPref>(Keys.ReactionPickerType)?.value)
            .let {
                when (it) {
                    0 -> {
                        ReactionPickerType.LIST
                    }
                    1 -> {
                        ReactionPickerType.SIMPLE
                    }
                    else -> {
                        DefaultConfig.config.reactionPickerType
                    }
                }
            },
        backgroundImagePath = map.getValue<PrefType.StrPref>(Keys.BackgroundImage)?.value
            ?: DefaultConfig.config.backgroundImagePath,
        isClassicUI = map.getValue<PrefType.BoolPref>(Keys.ClassicUI)?.value
            ?: DefaultConfig.config.isClassicUI,
        isUserNameDefault = map.getValue<PrefType.BoolPref>(Keys.IsUserNameDefault)?.value
            ?: DefaultConfig.config.isUserNameDefault,
        isPostButtonAtTheBottom = map.getValue<PrefType.BoolPref>(Keys.IsPostButtonToBottom)?.value
            ?: DefaultConfig.config.isPostButtonAtTheBottom,
        urlPreviewConfig = UrlPreviewConfig(
            type = UrlPreviewConfig.Type.from(
                map.getValue<PrefType.IntPref>(Keys.UrlPreviewSourceType)?.value
                    ?: UrlPreviewSourceSetting.MISSKEY,
                url = map.getValue<PrefType.StrPref>(Keys.SummalyServerUrl)?.value
            ),
        ),
        noteExpandedHeightSize = map.getValue<PrefType.IntPref>(Keys.NoteLimitHeight)?.value
            ?: DefaultConfig.config.noteExpandedHeightSize,
        theme = Theme.from(map.getValue<PrefType.IntPref>(Keys.ThemeType)?.value ?: 0),
        isIncludeRenotedMyNotes = map.getValue<PrefType.BoolPref>(Keys.IsIncludeRenotedMyNotes)?.value
            ?: DefaultConfig.config.isIncludeMyRenotes,
        isIncludeMyRenotes = map.getValue<PrefType.BoolPref>(Keys.IsIncludeMyRenotes)?.value
            ?: DefaultConfig.config.isIncludeMyRenotes,
        isIncludeLocalRenotes = map.getValue<PrefType.BoolPref>(Keys.IsIncludeLocalRenotes)?.value
            ?: DefaultConfig.config.isIncludeLocalRenotes,
        surfaceColorOpacity = map.getValue<PrefType.IntPref>(Keys.SurfaceColorOpacity)?.value
            ?: DefaultConfig.config.surfaceColorOpacity,
        isEnableTimelineScrollAnimation = map.getValue<PrefType.BoolPref>(Keys.IsEnableTimelineScrollAnimation)?.value
            ?: DefaultConfig.config.isEnableTimelineScrollAnimation
    )
}

private fun <T : PrefType?> Map<Keys, PrefType?>.getValue(key: Keys): T? {
    return this[key] as? T
}

fun Config.pref(key: Keys): PrefType? {
    return when (key) {
        Keys.BackgroundImage -> {
            PrefType.StrPref(backgroundImagePath)
        }
        Keys.ClassicUI -> {
            PrefType.BoolPref(isClassicUI)
        }
        Keys.IsPostButtonToBottom -> {
            PrefType.BoolPref(isPostButtonAtTheBottom)
        }
        Keys.IsSimpleEditorEnabled -> {
            PrefType.BoolPref(isSimpleEditorEnabled)
        }
        Keys.IsUserNameDefault -> {
            PrefType.BoolPref(isUserNameDefault)
        }
        Keys.NoteLimitHeight -> {
            PrefType.IntPref(noteExpandedHeightSize)
        }
        Keys.ReactionPickerType -> {
            PrefType.IntPref(
                when (reactionPickerType) {
                    ReactionPickerType.LIST -> 0
                    ReactionPickerType.SIMPLE -> 1
                }
            )
        }
        Keys.SummalyServerUrl -> {
            val type = urlPreviewConfig.type
            if (type is UrlPreviewConfig.Type.SummalyServer && urlPattern.matches(type.url)) {
                PrefType.StrPref(type.url)
            } else {
                null
            }
        }
        Keys.UrlPreviewSourceType -> {
            PrefType.IntPref(urlPreviewConfig.type.toInt())
        }
        Keys.ThemeType -> {
            PrefType.IntPref(theme.toInt())
        }
        Keys.IsIncludeLocalRenotes -> {
            PrefType.BoolPref(isIncludeLocalRenotes)
        }
        Keys.IsIncludeMyRenotes -> {
            PrefType.BoolPref(isIncludeMyRenotes)
        }
        Keys.IsIncludeRenotedMyNotes -> {
            PrefType.BoolPref(isIncludeRenotedMyNotes)
        }
        Keys.SurfaceColorOpacity -> {
            PrefType.IntPref(surfaceColorOpacity)
        }
        Keys.IsEnableTimelineScrollAnimation -> {
            PrefType.BoolPref(isEnableTimelineScrollAnimation)
        }
    }
}

fun Config.prefs(): Map<Keys, PrefType> {
    val map = mutableMapOf<Keys, PrefType>()
    Keys.allKeys.forEach { key ->
        pref(key)?.let {
            map[key] = it
        }
    }
    return map
}