package com.echo.virtual_interview.controller.ai.rag.util;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * XfyunKnowledgeHelper 的集成测试类。
 * <p>
 * 注意：这是一个集成测试，它会真实地调用讯飞的API接口。
 * 每个测试方法都是独立的，可以单独运行。
 */
class XfyunKnowledgeHelperTest {

    // =====================================================================================
    // ============================ ↓↓↓ 请在这里修改为您的真实凭证 ↓↓↓ ============================
    // =====================================================================================
    private static final String APP_ID = "bbbfe203";
    private static final String APP_SECRET = "***REMOVED***";
    // =====================================================================================

    private XfyunKnowledgeHelper helper;

    @BeforeEach
    void setUp() {
        // 在每个测试方法运行前，初始化Helper
        helper = new XfyunKnowledgeHelper(APP_ID, APP_SECRET);
    }

    /**
     * 独立测试：创建一个新的知识库。
     * <p>
     * 每次运行此测试都会创建一个新的、名称唯一的知识库。
     */
    @Test
    @DisplayName("独立测试：创建一个新的知识库")
    void createKnowledgeBase() throws IOException {
        // 使用动态名称以确保每次运行时都能成功创建，避免重名冲突
        String repoName = "interview";
        String repoDesc = "收录各大互联网公司（BAT/TMD等）及不同技术领域的面试题库，包含高频考点、真题解析和面试经验，涵盖Java/Python/前端/算法等多个技术方向";
        String repoTags = "面试题,大厂真题,求职,Java,Python,前端,算法,数据结构,数据库,系统设计,面经";

        System.out.println("尝试创建知识库，名称: " + repoName);
        XfyunKnowledgeHelper.BaseResponse<String> response = helper.createKnowledgeBase(repoName, repoDesc, repoTags);

        assertNotNull(response, "API响应不应为null");
        assertEquals(0, response.getCode(), "API响应码应为0，表示成功。错误信息: " + response.getDesc());
        assertNotNull(response.getData(), "返回的知识库ID不应为null");

        System.out.println("✅ 知识库创建成功！名称: " + repoName + ", Repo ID: " + response.getData());
    }

    /**
     * 独立测试：创建一个新知识库，并上传classpath:document/目录下的所有.md文件。
     * <p>
     * 此测试会完整执行“创建->上传->处理->添加”的流程。
     * ✅ 知识库创建成功！名称: interview, Repo ID: 1ef67505699245a493a40b35609e34ce
     */
    @Test
    @DisplayName("独立测试：创建知识库并上传所有MD文件")
    void createRepoAndUploadAllFiles() throws IOException, InterruptedException, URISyntaxException {
//        // --- 步骤 1: 创建一个新的知识库 ---
//        String repoName = "interview";
//        System.out.println("--- 步骤 1: 创建一个新的知识库 ---");
//        System.out.println("知识库名称: " + repoName);
//        XfyunKnowledgeHelper.BaseResponse<String> createResponse = helper.createKnowledgeBase(repoName, "用于存放MD文件的知识库", "Markdown,文档");
//        assertEquals(0, createResponse.getCode(), "创建知识库失败: " + createResponse.getDesc());
//        String repoId = createResponse.getData();
//        System.out.println("✅ 知识库创建成功！Repo ID: " + repoId);
        String repoId = "1ef67505699245a493a40b35609e34ce";  // 这个是名称为 interview  的repo id
        // --- 步骤 2: 查找并遍历所有.md文件 ---
        System.out.println("\n--- 步骤 2: 查找 classpath:document/ 目录下的所有.md文件 ---");
        URL resourceUrl = XfyunKnowledgeHelperTest.class.getResource("/document");
        assertNotNull(resourceUrl, "在classpath下找不到 /document 目录！请在 src/main/resources 或 src/test/resources下创建它。");

        List<File> mdFiles;
        try (Stream<Path> paths = Files.walk(Paths.get(resourceUrl.toURI()))) {
            mdFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".md"))
                    .map(Path::toFile)
                    .collect(Collectors.toList());
        }

        assertTrue(mdFiles.size() > 0, "在 /document 目录下没有找到任何.md文件！");
        System.out.println("找到 " + mdFiles.size() + " 个.md文件，准备上传...");

        List<String> processedFileIds = new ArrayList<>();

        // --- 步骤 3: 循环上传并等待处理 ---
        for (File file : mdFiles) {
            System.out.println("\n----------------------------------------------------");
            System.out.println("--- 正在上传文件: " + file.getName() + " ---");
            XfyunKnowledgeHelper.UploadResp uploadResp = helper.uploadFile(file);
            assertNotNull(uploadResp, "文件上传API响应不应为null");
            assertEquals(0, uploadResp.getCode(), "文件上传失败: " + uploadResp.getDesc());
            String currentFileId = uploadResp.getData().getFileId();
            System.out.println("✅ 文件上传成功！File ID: " + currentFileId);

            System.out.println("--- 等待文件向量化处理... ---");
            boolean processed = false;
            for (int i = 0; i < 20; i++) { // 最多等待 20 * 5 = 100 秒
                XfyunKnowledgeHelper.StatusResp statusResp = helper.queryFileStatus(currentFileId);
                String status = statusResp.getData().get(0).getFileStatus();
                System.out.println("查询文件状态... 当前状态: " + status);
                if ("vectored".equalsIgnoreCase(status)) {
                    System.out.println("✅ 文件处理完成！");
                    processedFileIds.add(currentFileId); // 处理成功后，加入列表
                    processed = true;
                    break;
                }
                if ("failed".equalsIgnoreCase(status)) {
                    fail("文件 " + file.getName() + " (ID: " + currentFileId + ") 处理失败！");
                }
                Thread.sleep(5000); // 每5秒查询一次
            }
            if (!processed) {
                fail("文件 " + file.getName() + " (ID: " + currentFileId + ") 超时未处理完成！");
            }
        }

        // --- 步骤 4: 将所有处理完成的文件一次性添加到知识库 ---
        System.out.println("\n----------------------------------------------------");
        System.out.println("--- 步骤 4: 将所有已处理文件添加到知识库 ---");
        assertFalse(processedFileIds.isEmpty(), "没有成功处理的文件，无法添加到知识库。");

        XfyunKnowledgeHelper.BaseResponse<Object> addFileResp = helper.addFilesToKnowledgeBase(repoId, processedFileIds);
        assertNotNull(addFileResp, "添加文件API响应不应为null");
        assertEquals(0, addFileResp.getCode(), "添加文件到知识库失败: " + addFileResp.getDesc());
        System.out.println("✅ " + processedFileIds.size() + " 个文件成功添加到知识库！");
    }

    /**
     * 独立测试：列出账号下的所有知识库
     */
    @Test
    @DisplayName("独立测试：列出所有知识库")
    void listKnowledgeBases() throws IOException {
        System.out.println("\n--- 查询账号下的知识库列表 ---");
        // 传入null或空字符串可以查询所有知识库
        XfyunKnowledgeHelper.BaseResponse<Object> response = helper.listKnowledgeBases(null);

        assertNotNull(response);
        assertEquals(0, response.getCode(), "查询知识库列表失败: " + response.getDesc());
        System.out.println("✅ 查询成功！知识库列表: ");
        // 使用Jackson美化输出
        System.out.println(new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(response.getData()));
    }

    /**
     * 独立工具：根据名称删除一个知识库（可选）
     * <p>
     * 默认禁用，以免误删。使用前请修改 repoNameToDelete 为您要删除的确切名称。
     */
    @Test
    @DisplayName("独立工具：根据名称删除知识库")
    @Disabled("默认禁用以防止意外删除，请修改名称并手动启用")
    void deleteKnowledgeBaseByName() throws IOException {
        // ↓↓↓ 使用前请修改为您要删除的知识库的确切名称 ↓↓↓
        String repoNameToDelete = "请填写要删除的知识库名称";

        if ("请填写要删除的知识库名称".equals(repoNameToDelete)) {
            fail("请先修改 repoNameToDelete 变量为您要删除的知识库的确切名称！");
        }

        System.out.println("\n--- 准备删除知识库: " + repoNameToDelete + " ---");

        // 1. 先列出所有知识库，找到要删除的那个的ID
        XfyunKnowledgeHelper.BaseResponse<Object> listResponse = helper.listKnowledgeBases(repoNameToDelete);
        // 注意：这里的listKnowledgeBases是模糊查询，我们需要精确匹配
        // 为了简化，我们假设模糊查询返回的第一个就是目标，但在生产环境中需要更严谨的匹配
        // 此处需要根据实际返回的JSON结构进行解析，这里假设返回的是一个List<Map>
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, String>> repos = (List<java.util.Map<String, String>>)((java.util.Map<String, Object>)listResponse.getData()).get("rows");

        String repoIdToDelete = repos.stream()
                .filter(repo -> repoNameToDelete.equals(repo.get("repoName")))
                .map(repo -> repo.get("repoId"))
                .findFirst()
                .orElse(null);

        assertNotNull(repoIdToDelete, "未找到名称为 '" + repoNameToDelete + "' 的知识库，无法删除。");

        // 2. 执行删除
        System.out.println("找到知识库ID: " + repoIdToDelete + "，正在执行删除...");
        XfyunKnowledgeHelper.BaseResponse<Object> deleteResponse = helper.deleteKnowledgeBase(repoIdToDelete);
        assertNotNull(deleteResponse);
        assertEquals(0, deleteResponse.getCode(), "删除知识库失败: " + deleteResponse.getDesc());
        System.out.println("✅ 知识库删除成功！");
    }
}
