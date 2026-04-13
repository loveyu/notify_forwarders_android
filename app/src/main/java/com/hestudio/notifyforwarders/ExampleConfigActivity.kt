package com.hestudio.notifyforwarders

import android.content.ClipData
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.hestudio.notifyforwarders.ui.theme.NotifyForwardersTheme
import com.hestudio.notifyforwarders.ui.webview.WebViewScreen

class ExampleConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val yamlContent = loadYamlFromResource()
        setContent {
            NotifyForwardersTheme {
                ExampleConfigScreen(
                    yamlContent = yamlContent,
                    onBackPressed = { finish() }
                )
            }
        }
    }

    private fun loadYamlFromResource(): String {
        return try {
            resources.openRawResource(R.raw.example_config)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            "Error loading YAML: ${e.message}"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExampleConfigScreen(
    yamlContent: String,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()
    val htmlContent = remember(yamlContent, isDarkTheme) {
        buildYamlHtml(yamlContent, isDarkTheme)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.example_config_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("YAML", yamlContent))
                        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                    }) {
                        Text("\uD83D\uDCCB", fontSize = 18.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WebViewScreen(
                htmlContent = htmlContent,
                isDarkTheme = isDarkTheme
            )
        }
    }
}

private fun buildYamlHtml(content: String, isDarkTheme: Boolean): String {
    val escapedContent = content
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    val themeAttr = if (isDarkTheme) "dark" else "light"

    return """
<!DOCTYPE html>
<html data-theme="$themeAttr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark-dimmed.min.css" media="(prefers-color-scheme: dark)">
    <style>
        :root {
            --bg-color: #ffffff;
            --text-color: #333333;
            --code-bg: #f6f8fa;
            --border-color: #d0d7de;
        }
        [data-theme="dark"] {
            --bg-color: #0d1117;
            --text-color: #c9d1d9;
            --code-bg: #161b22;
            --border-color: #30363d;
        }
        * { box-sizing: border-box; -webkit-tap-highlight-color: transparent; }
        html, body {
            margin: 0; padding: 0; height: 100%; overflow: hidden;
            -webkit-user-select: text; user-select: text;
        }
        body {
            background-color: var(--bg-color); color: var(--text-color);
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
            font-size: 14px; line-height: 1.6;
        }
        .content {
            height: 100%; overflow-y: auto; padding: 8px; -webkit-overflow-scrolling: touch;
        }
        .code-wrapper { position: relative; margin: 4px 0; }
        pre {
            background-color: var(--code-bg) !important;
            border: 1px solid var(--border-color);
            border-radius: 4px; padding: 12px !important;
            overflow-x: auto; margin: 0;
        }
        code {
            font-family: ui-monospace, SFMono-Regular, SF Mono, Menlo, Consolas, monospace;
            font-size: 12px;
        }
        pre code { background-color: transparent !important; padding: 0 !important; }
        ::-webkit-scrollbar { width: 8px; height: 8px; }
        ::-webkit-scrollbar-track { background: var(--bg-color); }
        ::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 4px; }
        ::-webkit-scrollbar-thumb:hover { background: #8b949e; }
    </style>
</head>
<body>
    <div class="content">
        <pre><code id="content" class="language-yaml">${escapedContent}</code></pre>
    </div>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
    <script>
        (function() {
            hljs.highlightElement(document.getElementById('content'));
        })();
    </script>
</body>
</html>
    """.trimIndent()
}
