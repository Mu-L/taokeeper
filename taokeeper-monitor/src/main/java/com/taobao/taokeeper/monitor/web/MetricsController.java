package com.taobao.taokeeper.monitor.web;
import static common.toolkit.constant.EmptyObjectConstant.EMPTY_STRING;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.taobao.taokeeper.model.ServerMetrics;
import common.toolkit.util.number.IntegerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import common.toolkit.entity.DateFormat;
import common.toolkit.exception.DaoException;
import common.toolkit.util.DateUtil;
import common.toolkit.util.StringUtil;
import common.toolkit.util.io.ServletUtil;

/**
 * Metrics
 * 
 * @author yinshi.nc@taobao.com
 * @since 2011-08-10
 */
@Controller
@RequestMapping("/metrics")
public class MetricsController extends BaseController {

	private static final Logger LOG = LoggerFactory.getLogger( MetricsController.class );

    @PostMapping("/")
	public ModelAndView reportPAGE( HttpServletRequest request, HttpServletResponse response, int clusterId, String server, String statDate ){

		try {
			clusterId = IntegerUtil.defaultIfError( clusterId, 1 );
			statDate  = StringUtil.defaultIfBlank( statDate, DateUtil.getNowTime( DateFormat.Date ) );
			
            ServerMetrics serverMetrics = serverMetricsDAO.queryLastedServerMetricsByServer(clusterId,server);

            这里开始要输出了

            ServletUtil.writeToResponse(response,content);

			Map<String, Object> model = new HashMap<String, Object>();
			model.put( "contentOfReport", contentOfReport );
			model.put("clusterId", clusterId );
			model.put("server", server );
			model.put("statDate", statDate );
			return new ModelAndView( "report/report", model );
			
		} catch (NumberFormatException e) {
			LOG.error( "不合法的clusterId：" + clusterId );
			ServletUtil.writeToResponse(response, "不合法的clusterId：" + clusterId );
			e.printStackTrace();
		} catch ( DaoException e ) {
			LOG.error( "Error when handle db: " + e.getMessage() );
			ServletUtil.writeToResponse(response, "Error when handle db: " + e.getMessage() );
			e.printStackTrace();
		} catch ( Exception e ) {
			LOG.error( "Server error : " + e.getMessage() );
			ServletUtil.writeToResponse(response, "Server error: " + e.getMessage() );
			e.printStackTrace();
		}
		
		return null;
	}

}
