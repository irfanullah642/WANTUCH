package com.example.wantuch.data.local.dao

import androidx.room.*
import com.example.wantuch.data.local.entities.StaffEntity
import com.example.wantuch.data.local.entities.StudentEntity
import com.example.wantuch.data.local.entities.ClassEntity
import com.example.wantuch.data.local.entities.SectionEntity
import com.example.wantuch.data.local.entities.InstitutionEntity
import com.example.wantuch.data.local.entities.DashboardEntity
import com.example.wantuch.data.local.entities.PortfolioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WantuchDao {

    // Students
    @Query("SELECT * FROM students WHERE institutionId = :institutionId")
    fun getStudents(institutionId: Int): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>): List<Long>

    @Query("DELETE FROM students WHERE institutionId = :institutionId")
    suspend fun deleteStudentsByInstitution(institutionId: Int): Int

    @Query("SELECT * FROM students WHERE id = :studentId")
    suspend fun getStudentById(studentId: Int): StudentEntity?

    @Update
    suspend fun updateStudent(student: StudentEntity): Int

    // Staff
    @Query("SELECT * FROM staff WHERE institutionId = :institutionId")
    fun getStaff(institutionId: Int): Flow<List<StaffEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStaff(staff: List<StaffEntity>): List<Long>

    @Query("DELETE FROM staff WHERE institutionId = :institutionId")
    suspend fun deleteStaffByInstitution(institutionId: Int): Int

    @Query("SELECT * FROM staff WHERE id = :staffId")
    suspend fun getStaffById(staffId: Int): StaffEntity?

    @Update
    suspend fun updateStaff(staff: StaffEntity): Int

    // Structure (Classes and Sections)
    @Query("SELECT * FROM classes WHERE institutionId = :institutionId")
    fun getClasses(institutionId: Int): Flow<List<ClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClasses(classes: List<ClassEntity>): List<Long>

    @Query("SELECT * FROM sections WHERE classId = :classId")
    fun getSections(classId: Int): Flow<List<SectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSections(sections: List<SectionEntity>): List<Long>

    // Institutions
    @Query("SELECT * FROM institutions")
    fun getAllInstitutions(): Flow<List<InstitutionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstitutions(institutions: List<InstitutionEntity>): List<Long>

    // Dashboards
    @Query("SELECT * FROM dashboards WHERE institutionId = :institutionId")
    fun getDashboard(institutionId: Int): Flow<DashboardEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDashboard(dashboard: DashboardEntity): Long

    // Portfolio
    @Query("SELECT * FROM portfolio WHERE id = 1")
    fun getPortfolio(): Flow<PortfolioEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolio(portfolio: PortfolioEntity): Long
}
