package scalus.utils

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

extension [T](future: Future[T])
  /** Extension method to block and await the result of a Future.
    *
    * @param timeout
    *   maximum duration to wait (default: infinite)
    * @return
    *   the result of the Future
    * @throws java.util.concurrent.TimeoutException
    *   if the timeout is exceeded
    * @throws Exception
    *   if the Future fails
    */
  def await(timeout: Duration = Duration.Inf): T = ???
  // Await.result(future, timeout)
