package com.example.httplab

data class Course(
    val id: Int,
    val title: String,
    val hours: Int,
    val online: Boolean,
    val teacher: Teacher,
    val topics: List<String>
)

data class Teacher(
    val name: String,
    val degree: String
)

data class CourseResponse(
    val courses: List<Course>,
    val total: Int
)