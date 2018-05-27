package com.taobao.taokeeper.monitor.web;
import com.taobao.taokeeper.common.GlobalInstance;
import com.taobao.taokeeper.model.ZooKeeperCluster;
import com.taobao.taokeeper.model.ZooKeeperStatusV2;
import common.toolkit.exception.DaoException;
import common.toolkit.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static common.toolkit.constant.EmptyObjectConstant.EMPTY_STRING;

/**
 * Control of ZooKeeperStatus 
 * @author yinshi.nc@taobao.com
 * @since 2011-08-10
 */

@Controller
@RequestMapping("/monitor")
public class ZooKeeperStatusController extends BaseController {

	private static final Logger LOG = LoggerFactory.getLogger( ZooKeeperStatusController.class );

    @GetMapping("/clusterStatus")
    public String clusterStatus(Model model,String clusterId) {

        clusterId = StringUtil.defaultIfBlank( clusterId, 1 + EMPTY_STRING );
        //ZooKeeperCluster zooKeeperCluster = GlobalInstance.getZooKeeperClusterByClusterId( Integer.parseInt( clusterId) );
        //Map<Integer, ZooKeeperCluster > zooKeeperClusterMap = GlobalInstance.getAllZooKeeperCluster();
        ZooKeeperCluster zooKeeperCluster;
        String handleMessage = "";
        try{
            zooKeeperCluster = zooKeeperClusterDAO.getZooKeeperClusterByCulsterId( Integer.parseInt( clusterId) );
        } catch (DaoException e) {
            throw new RuntimeException(e);
        }
        if(null == zooKeeperCluster){
            handleMessage = "No cluster which id="+clusterId;
            LOG.warn(handleMessage);
        }

        Map<String, ZooKeeperStatusV2> zooKeeperStatusMap = new HashMap<String, ZooKeeperStatusV2>();

        //Get ZooKeeperStatusV2 of each server
        List<String> serverList = zooKeeperCluster.getServerList();
        if( null != serverList ){
            for( String server : serverList ){
                //String ip = StringUtil.trimToEmpty( server.split( COLON )[0] );
                //获取自检状态
                int statusType = GlobalInstance.getZooKeeperStatusTypeByServer( server );
                ZooKeeperStatusV2 zooKeeperStatus = GlobalInstance.getZooKeeperStatusByServer(server);
                if( null != zooKeeperStatus ){
                    zooKeeperStatus.setStatusType( statusType );
                }else{
                    zooKeeperStatus = new ZooKeeperStatusV2();
                    zooKeeperStatus.setStatusType( statusType );
                }
                zooKeeperStatusMap.put( server, zooKeeperStatus );
            }
        }

        model.addAttribute("zooKeeperStatusMap", zooKeeperStatusMap);
        model.addAttribute("zooKeeperCluster", zooKeeperCluster);
        model.addAttribute("handleMessage", handleMessage);
        model.addAttribute("zooKeeperClusterMap", GlobalInstance.getAllZooKeeperCluster() );
        model.addAttribute( "timeOfUpdateZooKeeperStatusSet", GlobalInstance.timeOfUpdateZooKeeperStatusSet );
        //model.put("clusterRTStatsMap", ZooKeeperRTCollectJob.getRtStatus().get(zooKeeperCluster.getClusterId()));
        //model.put("clusterRTStats", ZooKeeperRTCollectJob.getClustRTStatus().get(zooKeeperCluster.getClusterId()));


        return "monitor/clusterStatus";
    }

}
