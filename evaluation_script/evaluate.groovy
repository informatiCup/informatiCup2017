import java.io.File

categories = ["DEV", "HW", "OTHER", "EDU", "WEB", "DATA", "DOCS"] as Set

def readInDataSet (dataSetName, categoryToRepo, repositories) {
  categories.each {
    categoryToRepo[it]= [] as Set
  }

  count = 1;
  new File(dataSetName).splitEachLine(' ') { line ->
      category = line[1]
      repo = line[0]
      if (!categories.contains(category)) {
        println "Error in line ${count} of data set ${dataSetName}: ${category} is not a known category"
        System.exit(1)
      }
      if (repositories.contains(repo)) {
        println "Error in line ${count}: ${repo} has already been classified"
        System.exit(1)
      }
      repositories << repo
      categoryToRepo[category] << repo
      ++ count;
  }
}

def printCategorization (categoryToRepo) {
  categories.each {
    println "Repos classified as ${it}:"
    categoryToRepo[it].each {println it}
  }
}

def printCSVHeader() {
  categoriesPrecisionAndRecall=categories.collect{"${it} PREC,${it} REC"}.join(",")
  println "DATA SET,AVG PREC,AVG REC,${categoriesPrecisionAndRecall}"
}

def precision (golden, evaluated) {
  return golden.intersect(evaluated).size() / evaluated.size()
}

def recall (golden, evaluated) {
  return golden.intersect(evaluated).size() / golden.size()
}

cli = new CliBuilder(usage: 'groovy evaluate.groovy <golden data set> [evaluated data sets] ...')
OptionAccessor opt = cli.parse(args)

if(opt.arguments().size() < 1) {
	cli.usage()
	return
}

goldenDataSet = [:]
Set goldenRepositories = new HashSet()
// always treat the first file as the golden data set
readInDataSet(args[0], goldenDataSet, goldenRepositories)
//printCategorization (goldenDataSet)

printCSVHeader()

for (dataSet in args) {
  evaluatedDataSet = [:]
  Set evaluatedRepositories = new HashSet()
  readInDataSet(dataSet, evaluatedDataSet, evaluatedRepositories)
  //printCategorization (evaluatedDataSet)

  if (!goldenRepositories.equals(evaluatedRepositories)) {
    println "Repositories in golden data set and evaluated data set ${dataSet} do not match."
    println "Repos only found in golden data set:"
    goldenRepositories.minus(evaluatedRepositories).each {println it}
    println "Repos only found in ${dataSet}:"
    evaluatedRepositories.minus(goldenRepositories).each {println it}
  }

  results=categories.collect{[precision(goldenDataSet[it], evaluatedDataSet[it]), recall (goldenDataSet[it], evaluatedDataSet[it])]}

  // calculate average precision and recall across categories
  avgPrec=results.sum({it[0]})/results.size()
  avgRec=results.sum({it[1]})/results.size()

  println "${dataSet},${avgPrec},${avgRec},${results.flatten().join(",")}"
}
