package vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;

/**
 * @author zhengbo
 * @date 2020/12/17 16:03
 */
public class UrlParamsVerticle extends AbstractVerticle {

  // 第一步 声明Router
  Router router;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // 第二步 初始化Router
    router = Router.router(vertx);

    // 第四步 配置Router解析url
    router.route("/").handler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    });

    // 第四步 配置Router解析url
    // http://localhost:8888/test?page=1&age=10
    // 经典模式 以?分隔url与params 以&分隔各参数
    router.route("/test").handler(req -> {
      // vert.x获取url参数 req.request().getParam()
      String page = req.request().getParam("page");
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x>>>>>>>>>>>> " + page);
    });

    // 第四步 配置Router解析url
    // http://localhost:8888/test/1/10
    // rest模式 纯粹以/分隔
    // url中带:表示参数
    router.route("/test/:page").handler(req -> {
      // vert.x获取url参数 req.request().getParam()
      String page = req.request().getParam("page");
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!>>>>>>>>>>>>rest>>>>>> " + page);
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
  }
}
