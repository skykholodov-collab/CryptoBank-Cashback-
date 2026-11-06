package kz.cashbackcases.core

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PolicyCopyTest {
    @Test
    fun productSourcesDoNotSellIncomeOrRealPartnerships() {
        val root = repoRoot()
        val files = listOf(root.resolve("core/src/main"), root.resolve("app/src/main"))
            .flatMap { dir ->
                dir.walkTopDown().filter { file ->
                    file.isFile && file.extension in setOf("kt", "xml")
                }.toList()
            }
        assertTrue(files.isNotEmpty())
        val banned = listOf(
            "гарантир",
            "инвест",
            "депозит",
            "лутбокс",
            "lootbox",
            "казино",
            "реферал",
            "таймер",
            "срочно",
            "пассив",
            "удво",
            "крипт",
            "crypto",
            "bitcoin",
            "kaspi",
            "halyk",
            "technodom",
            "sulpak",
            "magnum",
            "flyarystan",
            "air astana",
            "банк",
            "доллар",
        )
        val dollarAmount = Regex("""\$\s*\d""")
        val urgency = Regex("""2\s*час|два часа|двух часов""")
        val violations = mutableListOf<String>()
        files.forEach { file ->
            val text = file.readText()
            val lower = text.lowercase()
            banned.forEach { needle ->
                if (needle in lower) violations += "${file.relativeTo(root).path} contains «$needle»"
            }
            if (dollarAmount.containsMatchIn(text)) {
                violations += "${file.relativeTo(root).path} contains a dollar amount"
            }
            if (urgency.containsMatchIn(lower)) {
                violations += "${file.relativeTo(root).path} contains an urgency window"
            }
        }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    private fun repoRoot(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        repeat(6) {
            if (File(dir, "settings.gradle.kts").exists()) return dir
            dir = dir.parentFile ?: error("Repo root not found")
        }
        error("Repo root not found from ${System.getProperty("user.dir")}")
    }
}
