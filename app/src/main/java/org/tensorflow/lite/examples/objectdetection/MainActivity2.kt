package org.tensorflow.lite.examples.objectdetection

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMain2Binding
import org.tensorflow.lite.examples.objectdetection.databinding.ActivityMainBinding

class MainActivity2 : AppCompatActivity() {

    private lateinit var activityMain2Binding: ActivityMain2Binding
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>


    val TAKE_TYPE = "take_type" //拍摄类型标记

    val IMAGE_PATH = "image_path" //图片路径标记

    val TYPE_IDCARD_FRONT = 1 //身份证正面
    val TYPE_IDCARD_BACK = 2 //身份证反面
    val TYPE_LICENSE_PLATE = 3 //车牌


    val RESULT_CODE = 0X11 //结果码

    override fun onStart() {
        super.onStart()

//        activityMain2Binding.ivFrontButton.setOnClickListener {
//            resultLauncher =
//                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                    if (result.resultCode == Activity.RESULT_OK) {
//                        val data: Intent? = result.data
//                        data?.let {
//                            val result = it.getStringExtra("key")
//                            // 处理返回的数据
//                        }
//                    }
//                }
//            val intent = Intent(this, MainActivity::class.java)
//            resultLauncher.launch(intent)
//        }
//
//        activityMain2Binding.ivBackButton.setOnClickListener {
//            resultLauncher =
//                registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//                    if (result.resultCode == Activity.RESULT_OK) {
//                        val data: Intent? = result.data
//                        data?.let {
//                            val result = it.getStringExtra("key")
//                            // 处理返回的数据
//                        }
//                    }
//                }
//            val intent = Intent(this, MainActivity::class.java)
//            resultLauncher.launch(intent)
//        }


        // 其他初始化工作
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activityMain2Binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(activityMain2Binding.root)

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data: Intent? = result.data
                    data?.let {
                        val result = it.getStringExtra("key")
                        // 处理返回的数据
                    }
                }

                if (result.resultCode == RESULT_CODE) {
                    //获取图片路径，显示图片
                    val path: String? = getImagePath(result.data)
                    if (!TextUtils.isEmpty(path)) {

                        //实际开发中将图片上传到服务器成功后需要删除全部缓存图片
//                FileUtils.clearCache(this);
                    }
                }
            }
        activityMain2Binding.ivBackButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            resultLauncher.launch(intent)
        }


    }


    /**
     * 获取图片路径
     *
     * @param data Intent
     * @return 图片路径
     */
    private fun getImagePath(data: Intent?): String? {
        return if (data != null) {
            data.getStringExtra(IMAGE_PATH)
        } else ""
    }
}