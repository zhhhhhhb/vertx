package vertx;

import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.Log4JLoggerFactory;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.impl.Log4jLoggerFactory;

/**
 * @author zhengbo
 * @date 2020/12/21 14:57
 */
public class StaticFileVerticle extends AbstractVerticle {

  // 第一步 声明Router
  Router router;

  // 第一步声明模板引擎
  ThymeleafTemplateEngine thymeleafTemplateEngine;

  final InternalLogger logger = Log4JLoggerFactory.getInstance(StaticFileVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    // 第二步 初始化Router
    router = Router.router(vertx);

    // 第二步初始化模板引擎
    thymeleafTemplateEngine = ThymeleafTemplateEngine.create(vertx);

    // 整合静态文件，第一步
//    router.route("/static/*").handler(StaticHandler.create());

    // 整合静态文件，自定义访问路径
    router.route("/*").handler(StaticHandler.create());

    // 第四步 配置Router解析url
    router.route("/").handler(req -> {

      logger.error("error");

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
