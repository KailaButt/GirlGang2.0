package com.example.consolicalm

data class CalmSession(
    val id: String,
    val title: String,
    val description: String,
    val estimatedSeconds: Int,
    val steps: List<String>,
    val points: Int = 2
)

/**
 * Sessions used in the Calm tab. These are intentionally short and beginner-friendly.
 * Guided Calm Sessions now includes 10 total guided exercises so the tab feels fuller
 * without changing the surrounding UI.
 */
fun defaultCalmSessions(): List<CalmSession> = listOf(
    CalmSession(
        id = "grounding_54321",
        title = "5-4-3-2-1 grounding",
        description = "Use your senses to anchor back to the present moment.",
        estimatedSeconds = 180,
        steps = listOf(
            "Take one slow breath in… and a longer breath out.",
            "Name 5 things you can SEE.",
            "Name 4 things you can FEEL (clothing, chair, feet on the floor).",
            "Name 3 things you can HEAR.",
            "Name 2 things you can SMELL (or two smells you like).",
            "Name 1 thing you can TASTE (or one taste you like).",
            "Finish with one more slow breath out."
        )
    ),
    CalmSession(
        id = "box_breathing",
        title = "Box breathing (4-4-4-4)",
        description = "A steady rhythm that can calm your body fast.",
        estimatedSeconds = 150,
        steps = listOf(
            "Inhale through your nose for 4.",
            "Hold for 4.",
            "Exhale slowly for 4.",
            "Hold for 4.",
            "Repeat 3–5 rounds. Keep the exhale gentle."
        )
    ),
    CalmSession(
        id = "stop_skill",
        title = "STOP skill",
        description = "A quick reset when your mind is racing.",
        estimatedSeconds = 90,
        steps = listOf(
            "S — Stop. Pause for a moment.",
            "T — Take a breath. One slow inhale + longer exhale.",
            "O — Observe. What do you feel in your body? What thoughts are here?",
            "P — Proceed. Choose one small next step."
        )
    ),
    CalmSession(
        id = "breath_478",
        title = "4-7-8 relaxing breath",
        description = "A calming pattern to help settle stress and slow your mind.",
        estimatedSeconds = 150,
        steps = listOf(
            "Get comfortable. Rest your tongue behind your top front teeth.",
            "Inhale through your nose for 4.",
            "Hold for 7.",
            "Exhale through your mouth for 8.",
            "Repeat 3–4 rounds. If you feel lightheaded, return to normal breathing."
        )
    ),
    CalmSession(
        id = "mini_body_scan",
        title = "Mini body scan",
        description = "Release tension by checking in from head to toe.",
        estimatedSeconds = 240,
        steps = listOf(
            "Forehead/jaw: unclench.",
            "Shoulders: drop them down.",
            "Chest/belly: soften as you exhale.",
            "Hands: relax your grip.",
            "Legs/feet: feel them supported.",
            "Name one spot that feels calmer than before."
        )
    ),
    CalmSession(
        id = "pmr_quick",
        title = "Progressive muscle relax (quick)",
        description = "Tense then release to signal safety to your body.",
        estimatedSeconds = 300,
        steps = listOf(
            "Hands: tense 5s → release 10s.",
            "Shoulders: tense 5s → release 10s.",
            "Face: tense 5s → release 10s.",
            "Legs: tense 5s → release 10s.",
            "Feet: tense 5s → release 10s.",
            "End with a slow breath out and notice the difference."
        )
    ),
    CalmSession(
        id = "square_soften",
        title = "Soften your shoulders",
        description = "A guided tension release for shoulders, neck, and jaw.",
        estimatedSeconds = 180,
        steps = listOf(
            "Take a slow inhale and lift your shoulders gently.",
            "Exhale and let your shoulders drop.",
            "Roll your shoulders back two times.",
            "Relax your jaw and unclench your teeth.",
            "Notice whether your neck feels a little softer."
        )
    ),
    CalmSession(
        id = "self_compassion_minute",
        title = "One-minute self-compassion",
        description = "A gentle guided reset when you feel hard on yourself.",
        estimatedSeconds = 120,
        steps = listOf(
            "Put one hand on your chest or stomach.",
            "Breathe in slowly for 4 and out for 6.",
            "Say: This is a hard moment.",
            "Say: I can take this one step at a time.",
            "Say: Progress still counts today."
        )
    ),
    CalmSession(
        id = "counted_exhale",
        title = "Counted exhale reset",
        description = "A guided breathing exercise that makes the exhale longer than the inhale.",
        estimatedSeconds = 180,
        steps = listOf(
            "Inhale through your nose for 3.",
            "Exhale slowly for 5.",
            "Repeat the 3-in / 5-out rhythm for five rounds.",
            "Let your shoulders stay heavy and relaxed.",
            "Finish with one normal breath and notice the pause."
        )
    ),
    CalmSession(
        id = "mindful_reset",
        title = "Mindful reset before studying",
        description = "A short guided transition from scattered to ready.",
        estimatedSeconds = 240,
        steps = listOf(
            "Settle into your chair and place both feet on the floor.",
            "Take two slow breaths.",
            "Notice one thing you can see, hear, and feel right now.",
            "Name the next small task you want to do.",
            "Say it simply: I am starting with just this one step.",
            "Open your materials and begin."
        )
    )
)
