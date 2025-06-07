import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.lloir.ornaassistant.MainState
import com.lloir.ornaassistant.R
import com.lloir.ornaassistant.ScreenData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MyAccessibilityService : AccessibilityService() {

    private var state: MainState? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var lastEventTime = System.currentTimeMillis()

    private val debugEnabled: Boolean
        get() = getSharedPreferences("orna_settings", Context.MODE_PRIVATE)
            .getBoolean("debug_logs", false)

    override fun onCreate() {
        super.onCreate()
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        state = MainState(
            windowManager,
            applicationContext,
            inflater.inflate(R.layout.notification_layout, null),
            inflater.inflate(R.layout.wayvessel_overlay, null),
            inflater.inflate(R.layout.assess_layout, null),
            inflater.inflate(R.layout.kg_layout, null),
            this
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceScope.launch {
            while (true) {
                delay(1000)
                val now = System.currentTimeMillis()
                if (now - lastEventTime > 750) {
                    val root = rootInActiveWindow ?: continue
                    val screenDataList = arrayListOf<ScreenData>()
                    parseScreen(root, screenDataList, 0)

                    if (screenDataList.any { it.name.contains("ACQUIRED", ignoreCase = true) }) {
                        if (debugEnabled) Log.d("OrnaDebug", "Idle fallback triggered item screen parse")
                        screenDataList.forEach {
                            if (debugEnabled) Log.d("OrnaDebug", "Text: ${it.name} | Rect: ${it.rect}")
                        }
                        state?.processData("com.orna", screenDataList)
                        lastEventTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            lastEventTime = System.currentTimeMillis()
            when (it.eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED,
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    serviceScope.launch {
                        delay(500)
                        it.source?.let { rootNode ->
                            val screenDataList = arrayListOf<ScreenData>()
                            parseScreen(rootNode, screenDataList, 0)

                            if (debugEnabled) {
                                Log.d("OrnaDebug", "Event type: ${it.eventType}")
                                screenDataList.forEach { data ->
                                    Log.d("OrnaDebug", "Text: ${data.name} | Rect: ${data.rect}")
                                }
                            }

                            state?.processData(it.packageName?.toString() ?: "unknown", screenDataList)

                            if (screenDataList.any { it.name.contains("ACQUIRED", ignoreCase = true) }) {
                                delay(1000)
                                val refreshedRoot = rootInActiveWindow
                                if (debugEnabled) Log.d("OrnaDebug", "Refreshed root is ${refreshedRoot?.className}")
                                if (refreshedRoot != null) {
                                    val retryList = arrayListOf<ScreenData>()
                                    parseScreen(refreshedRoot, retryList, 0)
                                    if (debugEnabled) {
                                        retryList.forEach { data ->
                                            Log.d("OrnaDebug", "Retry Text: ${data.name} | Rect: ${data.rect}")
                                        }
                                    }
                                    state?.processData(packageName?.toString() ?: "unknown", retryList)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun parseScreen(
        node: AccessibilityNodeInfo?,
        dataList: ArrayList<ScreenData>,
        depth: Int
    ): Boolean {
        if (node == null || depth > 250) return false
        var isProcessed = false

        node.text?.toString()?.let { text ->
            if (text in setOf("DROP", "New", "SEND TO KEEP")) isProcessed = true

            val rect = Rect()
            node.getBoundsInScreen(rect)
            val processingTime = System.currentTimeMillis()
            dataList.add(ScreenData(text, rect, processingTime, 0, node))
        }

        if (!isProcessed) {
            val count = node.childCount
            for (i in 0 until count) {
                val child = node.getChild(i)
                if (child != null) {
                    val childProcessed = parseScreen(child, dataList, depth + 1)
                    if (childProcessed) {
                        isProcessed = true
                        break
                    }
                }
            }
        }
        node.recycle()
        return isProcessed
    }

    override fun onInterrupt() {
        state?.cleanup()
        Toast.makeText(this, "Orna Assistant Screen Reader is off", Toast.LENGTH_LONG).show()
    }
}