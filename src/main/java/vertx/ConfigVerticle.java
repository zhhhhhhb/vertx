package vertx;

import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;

import java.util.ArrayList;

/**
 * @author zhengbo
 * @date 2020/12/21 16:28
 */
public class ConfigVerticle extends AbstractVerticle {

  // 第一步 声明Router
  Router router;

  ConfigRetriever retriever = ConfigRetriever.create(vertx);

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

          Integer page = Integer.valueOf(req.request().getParam("page"));

          // Get a connection from the pool
          client.getConnection(ar1 -> {

            if (ar1.succeeded()) {

              System.out.println("Connected");

              // Obtain our connection
              SqlConnection conn = ar1.result();

              Integer offset = (page-1)*2;

              // All operations execute on the same connection
              conn
                .preparedQuery("SELECT SJID,pid,SFYX,ct FROM meas_sjzb_copy limit 2 offset ?")
                .execute(Tuple.of(offset), ar2 -> {
                  // Release the connection to the pool
                  // 必须手动释放
                  conn.close();
                  if (ar2.succeeded()) {

                    ArrayList<JsonObject> list = new ArrayList<>();
                    ar2.result().forEach(item -> {
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
                  } else {
                    req.response()
                      .putHeader("content-type", "text/plain")
                      .end("Hello from Vert.x! \n" + ar2.cause().toString());
                  }
                });
            } else {
              System.out.println("Could not connect: " + ar1.cause().getMessage());
            }
          });

//      req.response()
//        .putHeader("content-type", "text/plain")
//        .end("Hello from Vert.x! is down");
        });
      }
    });
  }
}
