package vertx;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;

import java.util.ArrayList;

/**
 * @author zhengbo
 * @date 2020/12/22 15:46
 */
public class HandleExceptionVerticle extends AbstractVerticle {

  // 第一步 声明Router
  Router router;

  // 第一个配置连接参数
  MySQLConnectOptions connectOptions;

  // 第二个 配置连接池 Pool options
  PoolOptions poolOptions = new PoolOptions()
    .setMaxSize(5);

  // 第三步创建客户端池 Create the client pool
  MySQLPool client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // 第二步 初始化Router
    router = Router.router(vertx);

    // 第四步 配置Router解析url
    router.get("/").handler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    });

    // 第三步 将Router与vertx HttpServer绑定
    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });

    // 加入这行代码
    ConfigRetriever retriever = ConfigRetriever.create(vertx);

    retriever.getConfig(ar -> {
      if (ar.failed()) {
        // Failed to retrieve the configuration
      } else {
        JsonObject config = ar.result();

        connectOptions = new MySQLConnectOptions()
          .setPort(Integer.parseInt(config.getValue("port").toString()))
          .setHost(config.getString("host"))
          .setDatabase(config.getString("database"))
          .setUser(config.getString("user"))
          .setPassword(config.getString("password"));

        // 初始化数据客户端池
        client = MySQLPool.pool(vertx, connectOptions, poolOptions);

        // 第四步 配置Router解析url
        router.route("/test/list").handler(req -> {

          Integer page;
          Object temp = req.request().getParam("page");
          if (temp == null) {
            page = 1;
          } else {
            page = Integer.valueOf(String.valueOf(temp));
          }
          Integer offset = (page-1)*2;

          // Get a connection from the pool
          // 第一步 获取数据库连接
          this.getCon()
            .compose(con -> this.getRows(con, offset))  // 第二步，用获取到的连接查询数据库
            .onSuccess(rows -> {
              // 第三步 加工查询出来的数据，并返回给客户端
              ArrayList<JsonObject> list = new ArrayList<>();
              rows.forEach(item -> {
                JsonObject json = new JsonObject();
                json.put("SJID", item.getValue("SJID"));
                json.put("pid", item.getValue("pid"));
                json.put("SFYX", item.getValue("SFYX"));
                json.put("ct", item.getValue("ct"));
                list.add(json);
              });
              req.response()
                .putHeader("content-type", "application/json")
                .end("Hello from Vert.x! \n" + list.toString());
            })
          .onFailure(throwable -> {
            //真实的项目开发中，我们可以在这里捕获Future+Promise异步调用链中的异常
            //在这里统一处理，比如写入日志文件
            req.response()
              .putHeader("content-type", "application/json")
              .end(throwable.toString());
          });
        });
      }
    });
  }

  // 第一步 获取数据库连接
  private Future<SqlConnection> getCon() {

    // 非常关键,固定写法
    Promise<SqlConnection> promise = Promise.promise();
    client.getConnection(ar1 -> {
      if (ar1.succeeded()) {
        System.out.println("Connected");
        // Obtain our connection
        SqlConnection conn = ar1.result();

        // 非常关键,固定写法
        promise.complete(conn);
      } else {
        System.out.println("连接数据库失败");
        //Vert.x帮我们捕捉异常后，我们只需要做接下来的处理即可，比如写入日志

        // 非常关键,固定写法
        promise.fail(ar1.cause()); //这里就相当于继续向上抛出异常，用Promise来向上抛出异常
      }
    });

    // 非常关键,固定写法
    return promise.future();
  }

  // 第二部 用获取到的连接来查询数据库
  private Future<RowSet<Row>> getRows(SqlConnection conn, Integer offset) {

    // 非常关键,固定写法
    Promise<RowSet<Row>> promise = Promise.promise();

    conn
      .preparedQuery("SELECT SJID,pid,SFYX,ct FROM meas_sjzb_copy limit 2 offset ?")
      .execute(Tuple.of(offset), ar2 -> {
        // Release the connection to the pool
        // 必须手动释放
        conn.close();
        if (ar2.succeeded()) {
          // 非常关键,固定写法
          promise.complete(ar2.result());
        } else {
          // 非常关键,固定写法
          promise.fail(ar2.cause());
        }
      });
    return promise.future();
  }
}
