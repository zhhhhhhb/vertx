package vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;

/**
 * @author zhengbo
 * @date 2020/12/21 9:56
 */
public class TemplateVerticle extends AbstractVerticle {

  // 第一步 声明Router
  Router router;

  // 第一步声明模板引擎
  ThymeleafTemplateEngine thymeleafTemplateEngine;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // 第二步 初始化Router
    router = Router.router(vertx);

    // 第二步初始化模板引擎
    thymeleafTemplateEngine = ThymeleafTemplateEngine.create(vertx);

    // 第四步 配置Router解析url
    router.route("/").handler(req -> {

      JsonObject obj = new JsonObject();
      obj.put("name", "Hello world from backend");

      // 第三步 thymeleafTemplateEngine.render()
      thymeleafTemplateEngine.render(obj,
        "templates/index.html",
        bufferAsyncResult -> {
          if (bufferAsyncResult.succeeded()) {
            req.response()
              .putHeader("content-type", "text/html")
              .end(bufferAsyncResult.result());
          } else {

          }
        });
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
