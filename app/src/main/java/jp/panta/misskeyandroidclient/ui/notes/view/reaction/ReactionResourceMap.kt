package jp.panta.misskeyandroidclient.ui.notes.view.reaction

import jp.panta.misskeyandroidclient.R

object ReactionResourceMap {
    val reactionDrawableMap = mapOf(
        "angry" to R.drawable.ic_reaction_angry,
        "confused" to R.drawable.ic_reaction_confused,
        "congrats" to R.drawable.ic_reaction_congrats,
        "hmm" to R.drawable.ic_reaction_hmm,
        "laugh" to R.drawable.ic_reaction_laugh,
        "like" to R.drawable.ic_reaction_like,
        "love" to R.drawable.ic_reaction_love,
        "pudding" to R.drawable.ic_reaction_pudding,
        "rip" to R.drawable.ic_reaction_rip,
        "surprise" to R.drawable.ic_reaction_surprise,
        "star" to R.drawable.ic_star

    )

    val reactionMap = mapOf(
        "angry" to "\uD83D\uDCA2",  // 💢
        "confused" to "\uD83D\uDE22", // 😢
        "congrats" to "\uD83C\uDF89", // 🎉
        "hmm" to "\uD83E\uDD14",    //  🤔
        "laugh" to "\uD83D\uDE06",  // 😆
        "like" to "\uD83D\uDC4D", // 👍
        "love" to "❤", //❤
        "pudding" to "\uD83C\uDF6E", // 🍮
        "rip" to "\uD83D\uDE07", // 😇
        "surprise" to "\uD83D\uDE2E", // 😮
        "star" to "⭐" // ⭐
    )

    val defaultReaction = listOf("angry", "confused", "congrats", "hmm", "laugh", "like", "love", "pudding", "rip", "surprise", "star")

}