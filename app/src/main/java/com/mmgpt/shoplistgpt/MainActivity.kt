package com.mmgpt.shoplistgpt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.Toast
import android.view.MotionEvent

import com.mmgpt.shoplistgpt.databinding.ActivityMainBinding
import com.mmgpt.shoplistgpt.databinding.StartPageBinding
import com.mmgpt.shoplistgpt.CategoryPagerAdapter
import com.mmgpt.shoplistgpt.ShoppingCategory
import android.view.Menu
import android.view.MenuItem

import com.opencsv.CSVReader
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var startBinding: StartPageBinding
    private lateinit var viewModel: ShoppingListViewModel
    private lateinit var pagerAdapter: CategoryPagerAdapter

    private var nextItemId = 0L
    private var nextCategoryId = 0L
    /** The mode on screen; null while the start page is showing. */
    private var currentMode: Mode? = null
    private var touchStartX = 0f

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                // grant yourself read permission for this URI
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                importFile(it)
            }
        }

    private val createFileLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        ) { uri ->
            uri?.let { exportXlsx(it) }
        }

    override fun onStop() {
        super.onStop()
        viewModel.saveState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // `startPage` is already generated as a StartPageBinding, so we can
        // reference it directly without rebinding it to a View.
        startBinding = binding.startPage

        // Use the toolbar defined in the layout as the Activity's ActionBar
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        ).get(ShoppingListViewModel::class.java)

        // Update next ID counters from any persisted data
        nextItemId = viewModel.categories.value
            ?.flatMap { it.items }
            ?.maxOfOrNull { it.id }?.plus(1) ?: 0L
        nextCategoryId = viewModel.categories.value
            ?.maxOfOrNull { it.id }?.plus(1) ?: 0L

        binding.viewPager.orientation = ViewPager2.ORIENTATION_HORIZONTAL
        setPagerMode(Mode.SHOP)

        val pagerRecyclerView = binding.viewPager.getChildAt(0) as RecyclerView
        pagerRecyclerView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - touchStartX
                    val threshold = 100
                    val lastIndex = pagerAdapter.itemCount - 1
                    when {
                        deltaX > threshold && binding.viewPager.currentItem == 0 -> {
                            binding.viewPager.currentItem = lastIndex
                            true
                        }
                        deltaX < -threshold && binding.viewPager.currentItem == lastIndex -> {
                            binding.viewPager.currentItem = 0
                            true
                        }
                        else -> false
                    }
                }
                else -> false
            }
        }

        var lastSize = viewModel.categories.value?.size ?: 0
        viewModel.categories.observe(this) { categories ->
            val newSize = categories.size
            if (newSize != lastSize) {
                pagerAdapter.notifyDataSetChanged()
                lastSize = newSize
            }
        }

        startBinding.btnShop.setOnClickListener { showMode(Mode.SHOP) }
        startBinding.btnImport.setOnClickListener { pickCsvFile() }
        startBinding.btnPlan.setOnClickListener { showMode(Mode.PLAN) }
        startBinding.btnExport.setOnClickListener { promptForExport() }

        showMode(null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                showMode(null)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (binding.viewPager.visibility == View.VISIBLE) {
            showMode(null)
        } else {
            super.onBackPressed()
        }
    }

    private fun pickCsvFile() {
        // allow CSV or Excel files
        filePickerLauncher.launch(
            arrayOf(
                "text/csv",
                "application/vnd.ms-excel",
                "text/comma-separated-values",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "*/*"
            )
        )
    }

    private fun importFile(uri: Uri) {
        val mime = contentResolver.getType(uri)
        when {
            mime == "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" ||
                uri.toString().endsWith(".xlsx", true) -> importXlsx(uri)
            else -> importCsv(uri)
        }
    }

    private fun importCsv(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = CSVReader(InputStreamReader(inputStream))
            val items = mutableListOf<ShoppingItem>()
            reader.forEachIndexed { index, line ->
                if (index == 0) return@forEachIndexed // skip header
                line.forEach { cell ->
                    val name = cell.trim()
                    if (name.isNotEmpty()) {
                        items.add(
                            ShoppingItem(
                                id = nextItemId++,
                                name = name
                            )
                        )
                    }
                }
            }
            // Group all items under a single category for CSV imports
            viewModel.setCategories(
                listOf(ShoppingCategory(nextCategoryId++, "Items", items))
            )
            showMode(Mode.SHOP)
        }
    }

    private fun importXlsx(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = XSSFWorkbook(inputStream)
            val sheet = workbook.getSheetAt(0)
            val name = sheet.sheetName
            val parsed = try {
                LocalDate.parse(name, DateTimeFormatter.ISO_DATE)
            } catch (e: Exception) {
                null
            }
            viewModel.setPlanDate(parsed)
            val categories = mutableListOf<ShoppingCategory>()
            val numColumns = sheet.getRow(0).lastCellNum.toInt()
            for (col in 0 until numColumns) {
                var heading: String? = null
                var items = mutableListOf<ShoppingItem>()
                for (rowIndex in 0..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex) ?: continue
                    val cell = row.getCell(col) ?: continue
                    val value = cell.toString().trim()
                    if (value.isEmpty()) continue
                    val cellStyle = cell.cellStyle
                    val font = workbook.getFontAt(cellStyle.fontIndexAsInt)
                    val isHeading = font.bold
                    val isStruck = font.strikeout
                    if (isHeading) {
                        if (heading != null) {
                            categories.add(
                                ShoppingCategory(nextCategoryId++, heading, items)
                            )
                            items = mutableListOf()
                        }
                        heading = value
                    } else {
                        items.add(
                            ShoppingItem(
                                id = nextItemId++,
                                name = value,
                                isChecked = isStruck,
                                isStruck = isStruck
                            )
                        )
                    }
                }
                if (heading != null) {
                    categories.add(
                        ShoppingCategory(nextCategoryId++, heading, items)
                    )
                }
            }
            workbook.close()
            viewModel.setCategories(categories)
            showMode(Mode.SHOP)
        }
    }

    private fun promptForExport() {
        val defaultName = "Shop-${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}.xlsx"
        createFileLauncher.launch(defaultName)
    }

    private fun exportXlsx(uri: Uri) {
        val categories = viewModel.categories.value ?: emptyList()
        val workbook = XSSFWorkbook()
        val plan = viewModel.planDate.value ?: LocalDate.now()
        val sheetName = plan.format(DateTimeFormatter.ISO_DATE)
        val sheet = workbook.createSheet(sheetName)

        val headerFont = workbook.createFont().apply { bold = true }
        val headerStyle = workbook.createCellStyle().apply { setFont(headerFont) }

        val strikeFont = workbook.createFont().apply { strikeout = true }
        val strikeStyle = workbook.createCellStyle().apply { setFont(strikeFont) }

        val flaggedStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val strikeFlaggedStyle = workbook.createCellStyle().apply {
            setFont(strikeFont)
            fillForegroundColor = IndexedColors.YELLOW.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        categories.forEachIndexed { col, category ->
            var row = sheet.getRow(0) ?: sheet.createRow(0)
            val headerCell = row.createCell(col)
            headerCell.setCellValue(category.heading)
            headerCell.cellStyle = headerStyle

            category.items.forEachIndexed { index, item ->
                row = sheet.getRow(index + 1) ?: sheet.createRow(index + 1)
                val cell = row.createCell(col)
                cell.setCellValue(item.name)
                when {
                    item.isFlagged && (item.isChecked || item.isStruck) -> cell.cellStyle = strikeFlaggedStyle
                    item.isFlagged -> cell.cellStyle = flaggedStyle
                    item.isChecked || item.isStruck -> cell.cellStyle = strikeStyle
                }
            }
            // Auto-sizing columns relies on java.awt classes which are not
            // available on Android and cause a crash. Use a fixed width instead.
            sheet.setColumnWidth(col, 6000)
        }

        contentResolver.openOutputStream(uri)?.use { output ->
            workbook.write(output)
        }
        workbook.close()
        Toast.makeText(this, "Exported", Toast.LENGTH_SHORT).show()
    }

    private fun setPagerMode(mode: Mode) {
        pagerAdapter = CategoryPagerAdapter(this, viewModel, mode)
        binding.viewPager.adapter = pagerAdapter
    }

    /** Shows the start page ([target] == null), or the pager in Shop or Plan mode. */
    private fun showMode(target: Mode?) {
        val leavingPlan = currentMode == Mode.PLAN && target != Mode.PLAN
        if (leavingPlan) {
            // Items checked off while planning are the ones already at home.
            viewModel.setCategories(
                viewModel.categories.value?.map { cat ->
                    cat.copy(items = cat.items.map { it.copy(isStruck = it.isChecked) })
                } ?: emptyList()
            )
        }
        if (target == Mode.PLAN) {
            val planned = viewModel.categories.value?.map { cat ->
                cat.copy(items = cat.items.map { it.copy(isStruck = true) })
            } ?: return
            viewModel.setCategories(planned)
            viewModel.setPlanDate(LocalDate.now())
        }

        val pagerMode = target ?: Mode.SHOP
        if (pagerAdapter.mode != pagerMode) setPagerMode(pagerMode)

        binding.startPage.root.visibility = if (target == null) View.VISIBLE else View.GONE
        binding.viewPager.visibility = if (target == null) View.GONE else View.VISIBLE
        supportActionBar?.title = titleFor(target)
        currentMode = target
    }

    private fun titleFor(mode: Mode?): String {
        val fmt = DateTimeFormatter.ofPattern("d MMM")
        return when (mode) {
            null -> getString(R.string.app_name)
            Mode.PLAN -> getString(R.string.plan_with_date, LocalDate.now().format(fmt))
            Mode.SHOP -> viewModel.planDate.value
                ?.let { getString(R.string.shop_with_date, it.format(fmt)) }
                ?: getString(R.string.shop)
        }
    }
}
