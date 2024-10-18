package org.tensorflow.lite.examples.objectdetection

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMain2Binding

class MainActivity2 : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMain2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

//        setSupportActionBar(binding.toolbar)


        //findNavController() 方法用于查找与给定的 NavHostFragment 相关联的 NavController 对象。在这里，
        // 它通过 R.id.nav_host_fragment_content_main 找到与主要导航组件相关联的 NavController
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        //：这个方法尝试导航一个步骤回到应用程序的导航图中的父级目的地。它会根据给定的 appBarConfiguration 来确定是否应该执行导航操作。
        setupActionBarWithNavController(navController, appBarConfiguration)

        //当用户点击 FAB 时，将执行接下来的代码块。
        binding.fab.setOnClickListener { view ->
            //是一个静态方法，用于创建并显示一个 Snackbar。
            //view 参数是指定 Snackbar 将被附加到的视图。
            //"Replace with your own action" 是 Snackbar 中显示的文本内容。
            //Snackbar.LENGTH_LONG 表示 Snackbar 显示的时长为长时间
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setActionTextColor(R.id.fab).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}