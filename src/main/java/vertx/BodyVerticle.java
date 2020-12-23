package vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author zhengbo
 * @date 2020/12/17 16:16
 */
public class BodyVerticle extends AbstractVerticle {

  // 第一步 声明Router
  Router router;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // 第二步 初始化Router
    router = Router.router(vertx);

    // 获取body参数，得先添加这句
    router.route().handler(BodyHandler.create());

    // 第三步 将Router与vertx HttpServer绑定
    vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });

    // 第四步 配置Router解析url
    router.route("/").handler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    });

    // form-data 格式
    // 请求头中的content-type: application/x-www-form-urlencoded
    router.route("/test/form").handler(req -> {
      // vert.x获取form-data 格式参数 req.request().getFormAttribute()
      String page = req.request().getFormAttribute("page");
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x>>>>>>>form-data>>>>> " + page);
    });

    // json 格式数据
    // 请求头中的content-type: application/json
    router.route("/test/json").handler(req -> {
      // vert.x获取json 格式数据参数 req.request().getBodyAsJson()
      JsonObject page = req.getBodyAsJson();
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!>>>>>>>>>>>>json>>>>>> " + page.getValue("page"));
    });
  }
}
