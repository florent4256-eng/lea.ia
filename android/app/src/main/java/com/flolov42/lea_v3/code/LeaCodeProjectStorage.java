package com.flolov42.lea_v3.code;

import com.flolov42.lea_v3.ui.*;
import com.flolov42.lea_v3.agents.*;
import com.flolov42.lea_v3.modes.*;
import com.flolov42.lea_v3.plus.gamification.*;
import com.flolov42.lea_v3.plus.lifestyle.*;
import com.flolov42.lea_v3.plus.learning.*;
import com.flolov42.lea_v3.plus.premium.*;
import com.flolov42.lea_v3.plus.connect.*;
import com.flolov42.lea_v3.bixby.*;
import com.flolov42.lea_v3.routines.*;
import com.flolov42.lea_v3.telephony.*;
import com.flolov42.lea_v3.code.*;
import com.flolov42.lea_v3.core.*;
import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.notifications.*;
import com.flolov42.lea_v3.utilities.*;


import android.content.Context;
import java.util.List;

public class LeaCodeProjectStorage {

    private final LeaAgentDatabase db;

    public LeaCodeProjectStorage(Context ctx) {
        this.db = LeaAgentDatabase.get(ctx);
    }

    public long createProject(String name, String description, String rootPath) {
        return db.createProject(name, description, rootPath);
    }

    public void updateStatus(long projectId, String status, String apkPath) {
        db.updateProjectStatus(projectId, status, apkPath);
    }

    public List<LeaAgentDatabase.ProjectRow> getAllProjects() {
        return db.getProjects();
    }

    public void deleteProject(long projectId) {
        db.deleteProject(projectId);
    }

    public void addConversationMessage(long projectId, String role, String content) {
        db.addMessage(projectId, role, content);
    }

    public List<LeaAgentDatabase.MessageRow> getConversation(long projectId) {
        return db.getMessages(projectId);
    }
}
