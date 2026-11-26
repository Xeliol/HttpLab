package com.example.httplab

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var etFilter: EditText
    private lateinit var btnFilter: Button
    private lateinit var btnSort: Button
    private lateinit var tvMin: TextView
    private lateinit var etNewTitle: EditText
    private lateinit var etNewHours: EditText
    private lateinit var etNewTeacher: EditText
    private lateinit var btnAdd: Button
    private lateinit var rvCourses: RecyclerView
    private lateinit var tvJson: TextView
    private lateinit var btnSave: Button
    private lateinit var btnLoad: Button

    private lateinit var moshi: Moshi
    private lateinit var adapter: CourseAdapter

    private val allCourses = mutableListOf<Course>()
    private val displayedCourses = mutableListOf<Course>()
    private var currentFilter: Int? = null
    private var isSortedDesc = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initMoshi()
        initRecyclerView()
        loadFromRaw()
        setupListeners()
    }

    private fun initViews() {
        etFilter = findViewById(R.id.etFilter)
        btnFilter = findViewById(R.id.btnFilter)
        btnSort = findViewById(R.id.btnSort)
        tvMin = findViewById(R.id.tvMin)
        etNewTitle = findViewById(R.id.etNewTitle)
        etNewHours = findViewById(R.id.etNewHours)
        etNewTeacher = findViewById(R.id.etNewTeacher)
        btnAdd = findViewById(R.id.btnAdd)
        rvCourses = findViewById(R.id.rvCourses)
        tvJson = findViewById(R.id.tvJson)
        btnSave = findViewById(R.id.btnSave)
        btnLoad = findViewById(R.id.btnLoad)
    }

    private fun initMoshi() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private fun initRecyclerView() {
        adapter = CourseAdapter()
        rvCourses.layoutManager = LinearLayoutManager(this)
        rvCourses.adapter = adapter
    }

    private fun loadFromRaw() {
        val json = resources.openRawResource(R.raw.data)
            .bufferedReader()
            .use { it.readText() }

        val response = moshi.adapter(CourseResponse::class.java).fromJson(json)!!
        allCourses.clear()
        allCourses.addAll(response.courses)

        displayedCourses.clear()
        displayedCourses.addAll(allCourses)

        updateRecyclerView()
        updateMin()
    }

    private fun setupListeners() {
        btnFilter.setOnClickListener {
            val filterText = etFilter.text.toString().trim()
            if (filterText.isEmpty()) {
                currentFilter = null
                displayedCourses.clear()
                displayedCourses.addAll(allCourses)
            } else {
                val threshold = filterText.toIntOrNull()
                if (threshold == null) {
                    Toast.makeText(this, "Введите число", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                currentFilter = threshold
                displayedCourses.clear()
                displayedCourses.addAll(allCourses.filter { it.hours > threshold })
            }
            if (isSortedDesc) {
                displayedCourses.sortByDescending { it.hours }
            }
            updateRecyclerView()
            updateMin()
        }

        btnSort.setOnClickListener {
            isSortedDesc = true
            displayedCourses.sortByDescending { it.hours }
            updateRecyclerView()
            updateMin()
        }

        btnAdd.setOnClickListener {
            val title = etNewTitle.text.toString().trim()
            val hoursStr = etNewHours.text.toString().trim()
            val teacherName = etNewTeacher.text.toString().trim()

            if (title.isEmpty() || hoursStr.isEmpty() || teacherName.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hours = hoursStr.toIntOrNull()
            if (hours == null) {
                Toast.makeText(this, "hours должно быть числом", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newId = (allCourses.maxOfOrNull { it.id } ?: 0) + 1
            val newCourse = Course(
                id = newId,
                title = title,
                hours = hours,
                online = false,
                teacher = Teacher(name = teacherName, degree = "не указана"),
                topics = emptyList()
            )
            allCourses.add(newCourse)

            if (currentFilter != null) {
                displayedCourses.clear()
                displayedCourses.addAll(allCourses.filter { it.hours > currentFilter!! })
            } else {
                displayedCourses.clear()
                displayedCourses.addAll(allCourses)
            }
            if (isSortedDesc) {
                displayedCourses.sortByDescending { it.hours }
            }

            updateRecyclerView()
            updateMin()

            // Сериализация всего списка в JSON с отступами
            val response = CourseResponse(allCourses, allCourses.size)
            val prettyAdapter = moshi.adapter(CourseResponse::class.java).indent("  ")
            val json = prettyAdapter.toJson(response)
            tvJson.text = json
            Log.d("HTTP_LAB", json)

            etNewTitle.text.clear()
            etNewHours.text.clear()
            etNewTeacher.text.clear()
        }

        btnSave.setOnClickListener {
            val response = CourseResponse(allCourses, allCourses.size)
            val prettyAdapter = moshi.adapter(CourseResponse::class.java).indent("  ")
            val json = prettyAdapter.toJson(response)

            val file = File(filesDir, "courses.json")
            file.writeText(json)

            Toast.makeText(this, "Сохранено в ${file.absolutePath}", Toast.LENGTH_SHORT).show()
            Log.d("HTTP_LAB", "Saved: $json")
        }

        btnLoad.setOnClickListener {
            val file = File(filesDir, "courses.json")
            if (!file.exists()) {
                Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val json = file.readText()
            val response = moshi.adapter(CourseResponse::class.java).fromJson(json)!!

            allCourses.clear()
            allCourses.addAll(response.courses)

            currentFilter = null
            isSortedDesc = false
            displayedCourses.clear()
            displayedCourses.addAll(allCourses)

            updateRecyclerView()
            updateMin()

            tvJson.text = json
            Log.d("HTTP_LAB", "Loaded: $json")
            Toast.makeText(this, "Загружено из файла", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRecyclerView() {
        adapter.submitList(displayedCourses.toList())
    }

    private fun updateMin() {
        if (displayedCourses.isEmpty()) {
            tvMin.text = "Минимум: нет данных"
            return
        }
        val minCourse = displayedCourses.minByOrNull { it.hours }!!
        tvMin.text = "Минимум hours: ${minCourse.hours} — ${minCourse.title}"
    }
}