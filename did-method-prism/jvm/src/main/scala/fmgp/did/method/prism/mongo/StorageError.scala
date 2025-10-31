package fmgp.did.method.prism.mongo

// Storage
case class StorageException(fail: StorageError) extends Exception(fail.toString())

sealed trait StorageError { //  extends MediatorError {
  def error: String
}

final case class StorageCollection(val error: String) extends StorageError
object StorageCollection {
  def apply(throwable: Throwable) = new StorageCollection(throwable.getClass.getName() + ":" + throwable.getMessage)
}

final case class StorageThrowable(val error: String) extends StorageError
object StorageThrowable {
  def apply(throwable: Throwable) = new StorageThrowable(throwable.getClass.getName() + ":" + throwable.getMessage)
}

final case class DuplicateMessage(val error: String) extends StorageError
object DuplicateMessage {
  val code = 11000
  def apply(throwable: Throwable) = new DuplicateMessage(throwable.getClass.getName() + ":" + throwable.getMessage)
}
