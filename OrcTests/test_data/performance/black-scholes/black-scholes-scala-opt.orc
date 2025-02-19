{- Black-scholes benchmark.
 - 
 -}

include "benchmark.inc"

import site Sequentialize = "orc.compile.orctimizer.Sequentialize"

import class BlackScholesResult = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesResult"
import class BlackScholesData = "orc.test.item.scalabenchmarks.blackscholes.BlackScholesData"
import class BlackScholes = "orc.test.item.scalabenchmarks.blackscholes.BlackScholes"

val compute = BlackScholes.compute
  
-- Lines: 9
def run(data) =
    val riskless = BlackScholesData.riskless()
    val volatility = BlackScholesData.volatility()
    val res = Array(data.length?)
    riskless >> volatility >> res >>
    for(0, data.length?) >i> Sequentialize() >> -- Inferable
      data(i)? >option>
      res(i) := compute(option.price(), option.strike(), option.maturity(), riskless, volatility) 
      >> stop ;
    res

val data = BlackScholesData.data()

benchmarkSized("Black-Scholes-scala-opt", data.length?, { data }, run, BlackScholesData.check)

{-
BENCHMARK
-}
  