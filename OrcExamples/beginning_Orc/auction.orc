{- auction.orc

EXERCISE:

Write a function which conducts an auction given
a list of "bidder" sites and a starting bid. An
auction consists of a number of bidding rounds
performed in sequence. A bidding round consists
of calling every bidder site in parallel, passing
the current maximum bid. Each bidder site may
return a higher value (a bid). The first caller
to return a higher bid sets a new maximum bid for
the next round. If no callers return a bid within
5 seconds, the auction (and round) ends. Your
function should return the value of the winning
bid.

SOLUTION:
-}

type Bid = Number

def auction(List[lambda(Bid) :: Bid], Bid) :: Bid
def auction(bidders, max) =
  val (done, bid) =
    Rwait(5000) >> (true, max)
    | each(bidders) >bidder>
      bidder(max) >bid>
      Ift(bid :> max) >>
      (false, bid)
  Println("Current bid: " +  max) >>
  ( if done then max else auction(bidders, bid) )


def bidder(Bid) :: lambda(Bid) :: Bid
def bidder(max) = 
  def f(n) = Ift(n <: max) >> n + 1
  f

auction(map(bidder, range(0,10)), 1)

{-
OUTPUT:
Current bid: 1
Current bid: 2
Current bid: 3
Current bid: 4
Current bid: 5
Current bid: 6
Current bid: 7
Current bid: 8
Current bid: 9
9
-}
