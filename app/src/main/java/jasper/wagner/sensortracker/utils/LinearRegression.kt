package jasper.wagner.sensortracker.utils
//
//
//import android.os.Build
//import androidx.annotation.RequiresApi
//import java.util.stream.Collectors
//import java.util.stream.IntStream
//
//import java.util.Arrays.asList
//
//
//object LinearRegression {
//
//    fun predictNext(): Double{
//        return 0.0
//    }
//
//
//    private val x = listOf(2, 3, 5, 7, 9, 11, 14) // Consecutive hours developer codes
//    private val y = asList(4, 5, 7, 10, 15, 20, 40) // Number of bugs produced
//
//    @RequiresApi(Build.VERSION_CODES.N)
//    private fun predictForValue(predictForDependentVariable: Int): Double {
//        check(x.size == y.size) { "Must have equal X and Y data points" }
//
//        val numberOfDataValues = x.size
//
//        val xSquared = x
//            .stream()
//            .map<Any> { position -> Math.pow(position!!.toDouble(), 2.0) }
//            .collect(Collectors.toList())
//
//        val xMultipliedByY = IntStream.range(0, numberOfDataValues)
//            .map({ i -> x[i.toInt()] * y[i.toInt()] })
//            .boxed()
//            .collect(Collectors.toList())
//
//        val xSummed = x
//            .stream()
//            .reduce { prev, next -> prev!! + next!! }
//            .get()
//
//        val ySummed = y
//            .stream()
//            .reduce { prev, next -> prev!! + next!! }
//            .get()
//
//        val sumOfXSquared = xSquared
//            .stream()
//            .reduce({ prev, next -> prev!! + next!! })
//            .get()
//
//        val sumOfXMultipliedByY = xMultipliedByY
//            .stream()
//            .reduce({ prev, next -> prev!! + next!! })
//            .get()
//
//        val slopeNominator = numberOfDataValues * sumOfXMultipliedByY - ySummed * xSummed
//        val slopeDenominator =
//            numberOfDataValues * sumOfXSquared - Math.pow(xSummed.toDouble(), 2.0)
//        val slope = slopeNominator / slopeDenominator!!
//
//        val interceptNominator = ySummed - slope!! * xSummed
//        val interceptDenominator = numberOfDataValues.toDouble()
//        val intercept = interceptNominator / interceptDenominator
//
//        return slope!! * predictForDependentVariable + intercept!!
//    }
//}