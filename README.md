# Пример испольования кастомной фабрики моделей
Сохраняем данные внутри модели, наследующей `AndroidViewModel` при помощи `SharedPreferences`
Вообще-то, неплохо бы создать [`room`](https://developer.android.com/training/data-storage/room), с SQLite, пример.

## Зачем это?
Цель примера, кастомизировать список параметров в классе модели и передать, например, имя
фрагмента, который её использует.
Полезно, если несколько фрагментов используют один и тот же класс модели, а данные желательно
сохранять отдельно для каждого фрагмента.

## Почему это плохо?
Этот пример — сплошное нарушение основных правил [архитектуры](https://developer.android.com/jetpack/guide) Android-приложений. 

Поскольку используется [`AndroidViewModel` (AVM)](https://developer.android.com/reference/androidx/lifecycle/AndroidViewModel).

Первая проблема — Если экземпляр приложения ссылается на них, это проблема из-за проблемы цикла ссылок.

Вторая проблема — тестирование приложения. В обычном модульном тесте сложно создать контекст приложения.

Так, что не стоит хранить контекст приложения в модели. 
Вообще, ничего от приложения не должно пребывать внутри модели. Увы, соблазн
запихать в модель сохранение данных выше всяких предосторожностей.

Наличие статического экземпляра контекста - зло, поскольку оно может вызвать
утечку памяти. 
Однако наличие статического экземпляра приложения - это не так плохо, как может показаться, 
потому что в запущенном приложении есть только один экземпляр приложения, а значит «всё под контролем»
Следовательно, использование и наличие экземпляра Application в определенном классе в целом не является 
проблемой. 

## Основные моменты
1. Значения параметра `regime` сохраняются в `SharedPreferences`. Обратите внимание, что `SharedPreferences` теперь получаются при помощи библиотеки `androidx` в виде «ленивого» значения:
```kotlin
    private val preferences: SharedPreferences by lazy {
        Log.d(TAG, "Application: ${getApplication<Application>()}")
        PreferenceManager.getDefaultSharedPreferences(getApplication())
    }
```   
Старый метод получения _устарел_  

2. Фабрика моделей используется в своём простейшем «изводе»
Для `ViewModel` дополнительный параметр — name. Название для сохранения в 
[`SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences) уникального значения.

```kotlin
class TestViewModelFactory(private val name:String): ViewModelProvider.NewInstanceFactory() {
    companion object {
        const val TAG:String = "TestViewModelFactory"
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "$name")
        return TestViewModel(name) as T
    }
}
```

3. Фабрика [`TestAndroidViewModelFactory.kt`](./app/src/main/java/com/grandfatherpikhto/testviewmodels/TestAndroidViewModelFactory.kt), 
   наследованная [`AndroidViewModel`](https://developer.android.com/reference/androidx/lifecycle/AndroidViewModel) 
   принимает дополнительный параметр, `name` --- имя для сохранения уникальных данных для каждого фрагмента:
```kotlin
class TestAndroidViewModelFactory(private val application: Application, private val name:String): ViewModelProvider.NewInstanceFactory() {
    companion object {
        const val TAG:String = "TestAndroidViewModelFactory"
    }
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        Log.d(TAG, "Create model instance: $name")
        return TestAndroidViewModel(application, name) as T
    }
}
```

4. Для простейшего случая, когда модель наследуется от [`ViewModel`](https://developer.android.com/topic/libraries/architecture/viewmodel),
   код создания модели будет выглядеть так:

```kotlin
    private val testViewModel:TestAndroidViewModel by viewModels<TestAndroidViewModel> {
        TestViewModelFactory("First")
    }
```

4. Модель создаётся при помощи `viewModels {...}`. Контекст приложения получается при помощи 
   `requireActivity().application`:
```kotlin
    private val testViewModel:TestAndroidViewModel by viewModels<TestAndroidViewModel> {
        TestAndroidViewModelFactory(requireActivity().application, "First")
    }
```
И во втором фрагменте:
```kotlin
    private val testViewModel:TestAndroidViewModel by viewModels<TestAndroidViewModel> {
        TestAndroidViewModelFactory(requireActivity().application, "Second")
    }
```
4. Тогда, внутри модели можно реализовать метод сохранения данных:
```kotlin
    fun store() {
        preferences.edit {
            _regime.value?.let { putInt(name + REGIME, it) }
        }
    }
```
Небольшая проблема в том, что функции обратного вызова жизненного цикла модели `onClear()`, 
равно, как и коллбэки жизненненого цикла фрагмента `onDestroy()` и `onDestroyView()` отрабатывают 
далеко не всегда.

Чтобы эти методы вызывались при завершении приложения, необходимо явно вызывать `finish()`. Всё, что у нас
есть из срабатывающего всегда --- это `onStart()`, `onStop()`, `onPause()`, `onResume()`.

В этом примере будем использовать `onStop()` в [`FirstFragment.kt`](./app/src/main/java/com/grandfatherpikhto/testviewmodels/FirstFragment.kt),
[`SecondFragment.kt`](./app/src/main/java/com/grandfatherpikhto/testviewmodels/SecondFragment.kt):
```kotlin
    override fun onStop() {
        super.onStop()
        testViewModel.store()
        Log.d(TAG, "onStop()")
    }
```

Собственно, почти всё. 
Осталось инициализировать `Number Wheel Picker` в классе фрагмента (
[`FirstFragment.kt`](./app/src/main/java/com/grandfatherpikhto/testviewmodels/FirstFragment.kt),
[`SecondFragment.kt`](./app/src/main/java/com/grandfatherpikhto/testviewmodels/SecondFragment.kt)
):

```kotlin
        binding.apply {
            buttonFirst.setOnClickListener {
                findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            }
            val regimes = arrayOf<String>("Off", "Tag", "Tail", "Water", "Blink")
            npRegimeFirst.displayedValues = regimes
            npRegimeFirst.minValue = 0
            npRegimeFirst.maxValue = regimes.size - 1
            npRegimeFirst.value = testViewModel.regime.value!!
            npRegimeFirst.setOnValueChangedListener { numberPicker, previous, value ->
                Log.d(TAG, "Regime: $value")
                if(testViewModel.regime.value != value) {
                    testViewModel.changeRegime(value)
                }
            }
            testViewModel.regime.observe(viewLifecycleOwner, { value ->
                if(value != npRegimeFirst.value) {
                    npRegimeFirst.value = value
                }
            })
        }
    }
```

Важный момент: при присваивании данных обязательна проверка неравенства между данными модели
и данными `Wheel Picker`. Если этого не сделать, возникнет закольцованный вызов между данными
модели и данными пикера.

## Без ответа
Стоит подгрузить полный набор библиотек `compose` 
```kotlin
    implementation 'androidx.compose.ui:ui:1.0.5'

    // Tooling support (Previews, etc.)
    implementation("androidx.compose.ui:ui-tooling:1.0.5")
    // Foundation (Border, Background, Box, Image, Scroll, shapes, animations, etc.)
    implementation("androidx.compose.foundation:foundation:1.0.5")
    // Material Design
    implementation("androidx.compose.material:material:1.0.5")
    // Material design icons
    implementation("androidx.compose.material:material-icons-core:1.0.5")
    implementation("androidx.compose.material:material-icons-extended:1.0.5")
    // Integration with observables
    implementation("androidx.compose.runtime:runtime-livedata:1.0.5")
    implementation("androidx.compose.runtime:runtime-rxjava2:1.0.5")
    implementation "androidx.compose.runtime:runtime:1.0.5"
```

, как начинает вылезать ошибка 
<details>
    <summary>Посмотреть</summary>

```kotlin
E/AndroidRuntime: FATAL EXCEPTION: main
    Process: com.grandfatherpikhto.testpreferences, PID: 29523
    java.lang.RuntimeException: Unable to start activity ComponentInfo{com.grandfatherpikhto.testpreferences/com.grandfatherpikhto.testpreferences.MainActivity}: android.view.InflateException: Binary XML file line #23 in com.grandfatherpikhto.testpreferences:layout/activity_main: Binary XML file line #18 in com.grandfatherpikhto.testpreferences:layout/content_main: Error inflating class fragment
        at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3782)
        at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3961)
        at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:91)
        at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:149)
        at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:103)
        at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2386)
        at android.os.Handler.dispatchMessage(Handler.java:107)
        at android.os.Looper.loop(Looper.java:213)
        at android.app.ActivityThread.main(ActivityThread.java:8178)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:513)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1101)
     Caused by: android.view.InflateException: Binary XML file line #23 in com.grandfatherpikhto.testpreferences:layout/activity_main: Binary XML file line #18 in com.grandfatherpikhto.testpreferences:layout/content_main: Error inflating class fragment
     Caused by: android.view.InflateException: Binary XML file line #18 in com.grandfatherpikhto.testpreferences:layout/content_main: Error inflating class fragment
     Caused by: java.lang.ClassCastException: com.grandfatherpikhto.testpreferences.databinding.FragmentFirstBinding cannot be cast to androidx.lifecycle.ViewModelStoreOwner
        at com.grandfatherpikhto.testpreferences.FirstFragment.onViewCreated(FirstFragment.kt:74)
        at androidx.fragment.app.Fragment.performViewCreated(Fragment.java:2987)
        at androidx.fragment.app.FragmentStateManager.createView(FragmentStateManager.java:546)
        at androidx.fragment.app.FragmentStateManager.moveToExpectedState(FragmentStateManager.java:282)
        at androidx.fragment.app.FragmentStore.moveToExpectedState(FragmentStore.java:112)
        at androidx.fragment.app.FragmentManager.moveToState(FragmentManager.java:1647)
        at androidx.fragment.app.FragmentManager.dispatchStateChange(FragmentManager.java:3128)
        at androidx.fragment.app.FragmentManager.dispatchViewCreated(FragmentManager.java:3065)
        at androidx.fragment.app.Fragment.performViewCreated(Fragment.java:2988)
        at androidx.fragment.app.FragmentStateManager.ensureInflatedView(FragmentStateManager.java:392)
        at androidx.fragment.app.FragmentStateManager.moveToExpectedState(FragmentStateManager.java:281)
        at androidx.fragment.app.FragmentLayoutInflaterFactory.onCreateView(FragmentLayoutInflaterFactory.java:140)
        at androidx.fragment.app.FragmentController.onCreateView(FragmentController.java:135)
        at androidx.fragment.app.FragmentActivity.dispatchFragmentsOnCreateView(FragmentActivity.java:319)
        at androidx.fragment.app.FragmentActivity.onCreateView(FragmentActivity.java:298)
        at android.view.LayoutInflater.tryCreateView(LayoutInflater.java:1079)
        at android.view.LayoutInflater.createViewFromTag(LayoutInflater.java:1007)
        at android.view.LayoutInflater.createViewFromTag(LayoutInflater.java:971)
        at android.view.LayoutInflater.rInflate(LayoutInflater.java:1133)
        at android.view.LayoutInflater.rInflateChildren(LayoutInflater.java:1094)
        at android.view.LayoutInflater.parseInclude(LayoutInflater.java:1273)
        at android.view.LayoutInflater.rInflate(LayoutInflater.java:1129)
        at android.view.LayoutInflater.rInflateChildren(LayoutInflater.java:1094)
        at android.view.LayoutInflater.inflate(LayoutInflater.java:692)
        at android.view.LayoutInflater.inflate(LayoutInflater.java:536)
E/AndroidRuntime:     at com.grandfatherpikhto.testpreferences.databinding.ActivityMainBinding.inflate(ActivityMainBinding.java:50)
        at com.grandfatherpikhto.testpreferences.databinding.ActivityMainBinding.inflate(ActivityMainBinding.java:44)
        at com.grandfatherpikhto.testpreferences.MainActivity.onCreate(MainActivity.kt:22)
        at android.app.Activity.performCreate(Activity.java:8086)
        at android.app.Activity.performCreate(Activity.java:8074)
        at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1313)
        at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3755)
        at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3961)
        at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:91)
        at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:149)
        at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:103)
        at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2386)
        at android.os.Handler.dispatchMessage(Handler.java:107)
        at android.os.Looper.loop(Looper.java:213)
        at android.app.ActivityThread.main(ActivityThread.java:8178)
        at java.lang.reflect.Method.invoke(Native Method)
        at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:513)
        at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1101)
I/Process: Sending signal. PID: 29523 SIG: 9
```
</details>

Пока, не знаю, какая именно библиотека вызывает этот косяк.

Со «штатным» набором библиотек, котоырй создаётся мастером генерации приложения, всё работает 
отлично

```kotlin
dependencies {

    implementation 'androidx.core:core-ktx:1.7.0'
    implementation 'androidx.appcompat:appcompat:1.4.0'
    implementation 'com.google.android.material:material:1.4.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.2'
    implementation 'androidx.navigation:navigation-fragment-ktx:2.3.5'
    implementation 'androidx.navigation:navigation-ui-ktx:2.3.5'
    implementation 'androidx.preference:preference-ktx:1.1.1'
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.3'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.4.0'
}
```
